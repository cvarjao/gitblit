package com.gitblit.markup;

import java.util.List;

import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.tracwiki.core.TracWikiLanguage;

import com.gitblit.Keys;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor.MarkupSyntax;


public class TracWikiMarkupSyntax extends LegacyMarkupSyntax {
	public TracWikiMarkupSyntax(GitBlitWebApp app) {
		super(app);
	}

	private final MarkupLanguage lang=new TracWikiLanguage();
	
	@Override
	public MarkupSyntax getSyntax() {
		return MarkupSyntax.TRACWIKI;
	}

	@Override
	public MarkupLanguage getMarkupLanguage() {
		return lang;
	}

	@Override
	public List<String> getExtensions() {
		return settings.getStrings(Keys.web.tracwikiExtensions);
	}

}
