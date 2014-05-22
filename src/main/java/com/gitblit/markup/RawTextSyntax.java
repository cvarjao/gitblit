package com.gitblit.markup;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.IStoredSettings;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor.MarkupDocument;
import com.gitblit.wicket.MarkupProcessor.MarkupSyntax;

public class RawTextSyntax extends AbstractMarkupSyntax implements MarkupParser {
	public RawTextSyntax(GitBlitWebApp app) {
		super(app);
	}
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public MarkupSyntax getSyntax() {
		return MarkupSyntax.PLAIN;
	}
	@Override
	public boolean canHandle(String documentExt, String documentPath) {
		return true;
	}
	
	@Override
	public void parse(IStoredSettings settings, final MarkupDocument doc, final String repositoryName, final String commitId) {
		doc.html = doc.markupText;
	}
	@Override
	public List<String> getExtensions() {
		return new ArrayList<String>();
	}

}
