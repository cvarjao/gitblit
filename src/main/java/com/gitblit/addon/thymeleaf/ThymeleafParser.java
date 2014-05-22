package com.gitblit.addon.thymeleaf;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateProcessingParameters;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.context.VariablesMap;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.dagger.DaggerContext;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.markup.AbstractMarkupSyntax;
import com.gitblit.markup.MockHttpServletRequest;
import com.gitblit.models.RefModel;
import com.gitblit.utils.JGitUtils;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor.MarkupDocument;
import com.gitblit.wicket.MarkupProcessor.MarkupSyntax;

import dagger.ObjectGraph;

public class ThymeleafParser extends AbstractMarkupSyntax{
	private final IRepositoryManager repositoryManager;
	private TemplateEngine engine;
	
	private static class MockWebContext extends Context implements IWebContext{
		private final HttpServletRequest request;
		private final HttpServletResponse response;
		private final ServletContext  servletContext;
		
		public MockWebContext(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
			this.request=request;
			this.response=response;
			this.servletContext=servletContext;
		}
		@Override
		public HttpServletRequest getHttpServletRequest() {
			return request;
		}

		@Override
		public HttpServletResponse getHttpServletResponse() {
			return response;
		}

		@Override
		public HttpSession getHttpSession() {
			return null;
		}

		@Override
		public ServletContext getServletContext() {
			return servletContext;
		}

		@Override
		public VariablesMap<String, String[]> getRequestParameters() {
			return null;
		}

		@Override
		public VariablesMap<String, Object> getRequestAttributes() {
			return null;
		}

		@Override
		public VariablesMap<String, Object> getSessionAttributes() {
			return null;
		}

		@Override
		public VariablesMap<String, Object> getApplicationAttributes() {
			return null;
		}
		
	}
	public ThymeleafParser(GitBlitWebApp app) {
		super(app);
		ServletContext context=app.getServletContext();
		ObjectGraph objectGraph = (ObjectGraph) context.getAttribute(DaggerContext.INJECTOR_NAME);
		this.repositoryManager = objectGraph.get(IRepositoryManager.class);
		
		TemplateResolver resolver = new TemplateResolver();
		//resolver = new ClassLoaderTemplateResolver();
		resolver.setTemplateMode("XHTML");
		resolver.setResourceResolver(new GitRepositoryResolver());
		this.engine = new TemplateEngine();
		engine.setTemplateResolver(resolver);
	}
	@Override
	public void parse(IStoredSettings settings, MarkupDocument doc, String repositoryName, String commitId) {

		
		StringWriter writer = new StringWriter();
		MockHttpServletRequest req=new MockHttpServletRequest();
		req.setContextPath(settings.getString(Keys.web._ROOT+".thymeleafContextPath", "")+"/"+repositoryName);
		IContext context = new MockWebContext(req, null, app.getServletContext());
		context.getVariables().put("repository", repositoryManager.getRepository(repositoryName, false));
		engine.process(doc.documentPath, context, writer);
		doc.html=writer.toString();
	}

	@Override
	public MarkupSyntax getSyntax() {
		return MarkupSyntax.MARKDOWN;
	}

	@Override
	public List<String> getExtensions() {
		return settings.getStrings(Keys.web._ROOT+".thymeleafExtensions");
	}
	public final class GitRepositoryResolver implements IResourceResolver {
		public static final String NAME = "GIT";
		
		public GitRepositoryResolver() {
		}
		@Override
		public String getName() {
			return NAME;
		}

		@Override
		public InputStream getResourceAsStream(TemplateProcessingParameters templateProcessingParameters, String resourceName) {
			// retrieve the content from the repository
			Repository r = (Repository) templateProcessingParameters.getContext().getVariables().get("repository");
			RefModel pages = JGitUtils.getPagesBranch(r);
			RevCommit commit = JGitUtils.getCommit(r, pages.getObjectId().getName());
			RevTree tree = commit.getTree();
			byte[] content=JGitUtils.getByteContent(r, tree, resourceName, false);
			if (content==null) content=new byte[0];
			return new ByteArrayInputStream(content);
		}
		

	}
}
