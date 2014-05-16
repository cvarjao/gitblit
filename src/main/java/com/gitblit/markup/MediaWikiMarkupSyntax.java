package com.gitblit.markup;

import java.util.List;

import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.mediawiki.core.MediaWikiLanguage;

import com.gitblit.Keys;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor.MarkupSyntax;


public class MediaWikiMarkupSyntax extends LegacyMarkupSyntax {
	public MediaWikiMarkupSyntax(GitBlitWebApp app) {
		super(app);
	}

	private final MarkupLanguage lang=new MediaWikiLanguage();
	
	@Override
	public MarkupSyntax getSyntax() {
		return MarkupSyntax.MEDIAWIKI;
	}

	@Override
	public List<String> getExtensions() {
		return settings.getStrings(Keys.web.mediawikiExtensions);
	}

	@Override
	public MarkupLanguage getMarkupLanguage() {
		return lang;
	}

}
