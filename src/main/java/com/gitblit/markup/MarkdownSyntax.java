package com.gitblit.markup;

import static org.pegdown.FastEncoder.encode;

import java.util.List;

import org.pegdown.LinkRenderer;
import org.pegdown.ast.ExpImageNode;
import org.pegdown.ast.RefImageNode;
import org.pegdown.ast.WikiLinkNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.utils.MarkdownUtils;
import com.gitblit.utils.StringUtils;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor.MarkupDocument;
import com.gitblit.wicket.MarkupProcessor.MarkupSyntax;
import com.gitblit.wicket.pages.DocPage;
import com.gitblit.wicket.pages.RawPage;

public class MarkdownSyntax extends AbstractMarkupSyntax implements MarkupParser {
	public MarkdownSyntax(GitBlitWebApp app) {
		super(app);
	}

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public MarkupSyntax getSyntax() {
		return MarkupSyntax.MARKDOWN;
	}
	
	@Override
	public void parse(final IStoredSettings settings, final MarkupDocument doc, final String repositoryName, final String commitId) {
		LinkRenderer renderer = new LinkRenderer() {

			@Override
			public Rendering render(ExpImageNode node, String text) {
				if (node.url.indexOf("://") == -1) {
					// repository-relative image link
					String path = doc.getRelativePath(node.url);
					String url = getWicketUrl(RawPage.class, repositoryName, commitId, path);
					return new Rendering(url, text);
				}
				// absolute image link
				return new Rendering(node.url, text);
			}

			@Override
			public Rendering render(RefImageNode node, String url, String title, String alt) {
				Rendering rendering;
				if (url.indexOf("://") == -1) {
					// repository-relative image link
					String path = doc.getRelativePath(url);
					String wurl = getWicketUrl(RawPage.class, repositoryName, commitId, path);
					rendering = new Rendering(wurl, alt);
				} else {
					// absolute image link
					rendering = new Rendering(url, alt);
				}
				return StringUtils.isEmpty(title) ? rendering : rendering.withAttribute("title", encode(title));
			}

			@Override
			public Rendering render(WikiLinkNode node) {
				String path = doc.getRelativePath(node.getText());
				String name = getDocumentName(path);
				String url = getWicketUrl(DocPage.class, repositoryName, commitId, path);
				return new Rendering(url, name);
			}
		};
		doc.html = MarkdownUtils.transformMarkdown(doc.markupText, renderer);
	}

	@Override
	public List<String> getExtensions() {
		return settings.getStrings(Keys.web.markdownExtensions);
	}

}
