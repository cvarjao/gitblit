package com.gitblit.markup;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.Constants;
import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.git.GitRevContext;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.models.PathModel;
import com.gitblit.models.RefModel;
import com.gitblit.utils.ByteFormat;
import com.gitblit.utils.JGitUtils;
import com.gitblit.utils.MarkdownUtils;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor;

import dagger.ObjectGraph;

public class DefaultPagesController implements PagesController {
	private transient Logger logger = LoggerFactory.getLogger(DefaultPagesController.class);
	private IRepositoryManager repositoryManager;
	private IStoredSettings settings;
	private GitBlitWebApp app;

	@Override
	public void init(ObjectGraph dagger) {
		this.settings = dagger.get(IStoredSettings.class);
		this.repositoryManager = dagger.get(IRepositoryManager.class);
		this.app = dagger.get(GitBlitWebApp.class);
	}

	private void error(HttpServletResponse response, String mkd) throws ServletException, IOException, ParseException {
		String content = MarkdownUtils.transformMarkdown(mkd);
		response.setContentType("text/html; charset=" + Constants.ENCODING);
		response.getWriter().write(content);
	}
	
	protected MarkupProcessor createMarkupProcessor(){
		return new MarkupProcessor(app);
	}
	
	protected PageResourceContext createPageResourceContext(ServletContext context, MarkupProcessor processor, GitRevContext revCtx, String path){
		String[] encodings = settings.getStrings(Keys.web.blobEncodings).toArray(new String[0]);
		return new PageResourceContext(context, processor, revCtx, path, encodings);
	}
	/**
	 * Retrieves the specified resource from the gh-pages branch of the repository.
	 * 
	 * @param request
	 * @param response
	 * @throws javax.servlet.ServletException
	 * @throws java.io.IOException
	 */
	@Override
	public void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (path.toLowerCase().endsWith(".git")) {
			// forward to url with trailing /
			// this is important for relative pages links
			response.sendRedirect(request.getServletPath() + path + "/");
			return;
		}
		if (path.charAt(0) == '/') {
			// strip leading /
			path = path.substring(1);
		}

		// determine repository and resource from url
		String repository = "";
		String resource = "";
		Repository r = null;
		int offset = 0;
		while (r == null) {
			int slash = path.indexOf('/', offset);
			if (slash == -1) {
				repository = path;
			} else {
				repository = path.substring(0, slash);
			}
			r = repositoryManager.getRepository(repository, false);
			offset = slash + 1;
			if (offset > 0) {
				resource = path.substring(offset);
			}
			if (repository.equals(path)) {
				// either only repository in url or no repository found
				break;
			}
		}

		ServletContext context = request.getSession().getServletContext();

		try {
			if (r == null) {
				// repository not found!
				String mkd = MessageFormat.format("# Error\nSorry, no valid **repository** specified in this url: {0}!", repository);
				error(response, mkd);
				return;
			}

			// retrieve the content from the repository
			RefModel pages = JGitUtils.getPagesBranch(r);
			RevCommit commit = JGitUtils.getCommit(r, pages.getObjectId().getName());

			if (commit == null) {
				// branch not found!
				String mkd = MessageFormat.format("# Error\nSorry, the repository {0} does not have a **gh-pages** branch!", repository);
				error(response, mkd);
				return;
			}

			RevTree tree = commit.getTree();

			String res = resource;
			if (res.endsWith("/")) {
				res = res.substring(0, res.length() - 1);
			}
			
			MarkupProcessor processor = createMarkupProcessor();
			GitRevContext revCtx1=new GitRevContext(GitRevContext.createNamedRepository(repositoryManager, repository, r), pages, commit, tree);
			PageResourceContext resContext=createPageResourceContext(context, processor, revCtx1, path);
			
			List<PathModel> pathEntries = JGitUtils.getFilesInPath(r, res, commit);
			
			//byte[] content = null;
			if (pathEntries.isEmpty()) {
				resContext.fetchResourceContent();
			} else {
				// path request
				if (!request.getPathInfo().endsWith("/")) {
					// redirect to trailing '/' url
					response.sendRedirect(request.getServletPath() + request.getPathInfo() + "/");
					return;
				}

				Map<String, String> names = new TreeMap<String, String>();
				for (PathModel entry : pathEntries) {
					names.put(entry.name.toLowerCase(), entry.name);
				}

				List<String> extensions = new ArrayList<String>();
				extensions.add("html");
				extensions.add("htm");
				extensions.addAll(processor.getMarkupExtensions());
				for (String ext : extensions) {
					String key = "index." + ext;

					if (names.containsKey(key)) {
						String fileName = names.get(key);
						String fullPath = fileName;
						if (!res.isEmpty()) {
							fullPath = res + "/" + fileName;
						}
						
						if (resContext.fetchResourceContent(fullPath, true)) {
							resContext.setContentType("text/html; charset=" + Constants.ENCODING);
							resContext.setPath(fullPath);
							break;
						}
					}
				}
			}

			// no content, document list or custom 404 page
			if (!resContext.hasContent()) {
				if (pathEntries.isEmpty()) {
					// 404
					if (resContext.fetchResourceContent("404.html", true)){
						resContext.setContentType("text/html; charset=" + Constants.ENCODING);
					}
					resContext.setContentType("text/html; charset=" + Constants.ENCODING);

					// still no content
					if (!resContext.hasContent()) {
						String str = MessageFormat.format("# Error\nSorry, the requested resource **{0}** was not found.", resource);
						resContext.setContent(str.getBytes(Constants.ENCODING));
					}

					try {
						// output the content
						logger.warn("Pages 404: " + resource);
						response.setContentType(resContext.getContentType());
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						response.getOutputStream().write(resContext.getContent());
						response.flushBuffer();
					} catch (Throwable t) {
						logger.error("Failed to write page to client", t);
					}
				} else {
					// document list
					response.setContentType("text/html");
					response.getWriter().append("<style>table th, table td { min-width: 150px; text-align: left; }</style>");
					response.getWriter().append("<table>");
					response.getWriter().append("<thead><tr><th>path</th><th>mode</th><th>size</th></tr>");
					response.getWriter().append("</thead>");
					response.getWriter().append("<tbody>");
					String pattern = "<tr><td><a href=\"{0}/{1}\">{1}</a></td><td>{2}</td><td>{3}</td></tr>";
					final ByteFormat byteFormat = new ByteFormat();
					if (!pathEntries.isEmpty()) {
						if (pathEntries.get(0).path.indexOf('/') > -1) {
							// we are in a subdirectory, add parent directory
							// link
							pathEntries.add(0, new PathModel("..", resource + "/..", 0, FileMode.TREE.getBits(), null, null));
						}
					}

					String basePath = request.getServletPath() + request.getPathInfo();
					if (basePath.charAt(basePath.length() - 1) == '/') {
						// strip trailing slash
						basePath = basePath.substring(0, basePath.length() - 1);
					}
					for (PathModel entry : pathEntries) {
						response.getWriter().append(
								MessageFormat.format(pattern, basePath, entry.name, JGitUtils.getPermissionsFromMode(entry.mode),
										byteFormat.format(entry.size)));
					}
					response.getWriter().append("</tbody>");
					response.getWriter().append("</table>");
				}
				return;
			}

			try {
				// output the content
				response.setContentType(resContext.getContentType());
				response.setHeader("Cache-Control", "public, max-age=3600, must-revalidate");
				response.setDateHeader("Last-Modified", JGitUtils.getCommitDate(commit).getTime());
				response.getOutputStream().write(resContext.getContent());
				response.flushBuffer();
			} catch (Throwable t) {
				logger.error("Failed to write page to client", t);
			}

		} catch (Throwable t) {
			logger.error("Failed to write page to client", t);
		} finally {
			r.close();
		}
	}
}
