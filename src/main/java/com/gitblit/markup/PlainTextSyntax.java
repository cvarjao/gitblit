package com.gitblit.markup;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.IStoredSettings;
import com.gitblit.utils.MarkdownUtils;
import com.gitblit.utils.StringUtils;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor.MarkupDocument;
import com.gitblit.wicket.MarkupProcessor.MarkupSyntax;

public class PlainTextSyntax extends AbstractMarkupSyntax implements MarkupParser {
	public PlainTextSyntax(GitBlitWebApp app) {
		super(app);
	}
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public MarkupSyntax getSyntax() {
		return MarkupSyntax.PLAIN;
	}
	
	@Override
	public void parse(final IStoredSettings settings, final MarkupDocument doc, final String repositoryName, final String commitId) {
		doc.html = MarkdownUtils.transformPlainText(doc.markupText);
	}
	@Override
	public List<String> getExtensions() {
		return StringUtils.getStringsFromValue("txt", " ");
	}

}
