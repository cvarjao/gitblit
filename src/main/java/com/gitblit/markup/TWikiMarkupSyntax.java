package com.gitblit.markup;

import java.util.List;

import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.twiki.core.TWikiLanguage;

import com.gitblit.Keys;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor.MarkupSyntax;


public class TWikiMarkupSyntax extends LegacyMarkupSyntax {
	public TWikiMarkupSyntax(GitBlitWebApp app) {
		super(app);
	}

	private final MarkupLanguage lang=new TWikiLanguage();
	
	@Override
	public MarkupSyntax getSyntax() {
		return MarkupSyntax.TWIKI;
	}

	@Override
	public MarkupLanguage getMarkupLanguage() {
		return lang;
	}

	@Override
	public List<String> getExtensions() {
		return settings.getStrings(Keys.web.twikiExtensions);
	}

}
