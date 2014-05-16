package com.gitblit.markup;

import java.util.List;

import org.eclipse.mylyn.wikitext.confluence.core.ConfluenceLanguage;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;

import com.gitblit.Keys;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor.MarkupSyntax;


public class ConfluenceMarkupSyntax extends LegacyMarkupSyntax {
	public ConfluenceMarkupSyntax(GitBlitWebApp app) {
		super(app);
	}
	private final MarkupLanguage lang=new ConfluenceLanguage();
	
	@Override
	public MarkupSyntax getSyntax() {
		return MarkupSyntax.CONFLUENCE;
	}

	@Override
	public MarkupLanguage getMarkupLanguage() {
		return lang;
	}
	@Override
	public List<String> getExtensions() {
		return settings.getStrings(Keys.web.confluenceExtensions);
	}

}
