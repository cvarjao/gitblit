package com.gitblit.markup;

import java.io.StringWriter;

import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;

import com.gitblit.IStoredSettings;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor.MarkupDocument;
import com.gitblit.wicket.pages.DocPage;
import com.gitblit.wicket.pages.RawPage;

public abstract class LegacyMarkupSyntax extends AbstractMarkupSyntax {
	
	public LegacyMarkupSyntax(GitBlitWebApp app) {
		super(app);
	}



	public abstract MarkupLanguage getMarkupLanguage();
	

	
	/**
	 * Parses the markup using the specified markup language
	 *
	 * @param doc
	 * @param repositoryName
	 * @param commitId
	 */
	@Override
	public void parse(final IStoredSettings settings, final MarkupDocument doc, final String repositoryName, final String commitId) {
		StringWriter writer = new StringWriter();
		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer) {

			@Override
			public void image(Attributes attributes, String imagePath) {
				String url;
				if (imagePath.indexOf("://") == -1) {
					// relative image
					String path = doc.getRelativePath(imagePath);
					url = getWicketUrl(RawPage.class, repositoryName, commitId, path);
				} else {
					// absolute image
					url = imagePath;
				}
				super.image(attributes, url);
			}

			@Override
			public void link(Attributes attributes, String hrefOrHashName, String text) {
				String url;
				if (hrefOrHashName.charAt(0) != '#') {
					if (hrefOrHashName.indexOf("://") == -1) {
						// relative link
						String path = doc.getRelativePath(hrefOrHashName);
						url = getWicketUrl(DocPage.class, repositoryName, commitId, path);
					} else {
						// absolute link
						url = hrefOrHashName;
					}
				} else {
					// page-relative hash link
					url = hrefOrHashName;
				}
				super.link(attributes, url, text);
			}
		};

		// avoid the <html> and <body> tags
		builder.setEmitAsDocument(false);

		MarkupParser parser = new MarkupParser(getMarkupLanguage());
		parser.setBuilder(builder);
		parser.parse(doc.markupText);
		doc.html = writer.toString();
	}
}
