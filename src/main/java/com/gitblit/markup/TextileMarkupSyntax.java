package com.gitblit.markup;

import java.util.List;

import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.textile.core.TextileLanguage;

import com.gitblit.Keys;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.MarkupProcessor.MarkupSyntax;


public class TextileMarkupSyntax extends LegacyMarkupSyntax {
	public TextileMarkupSyntax(GitBlitWebApp app) {
		super(app);
	}

	private final MarkupLanguage lang=new TextileLanguage();
	
	@Override
	public MarkupSyntax getSyntax() {
		return MarkupSyntax.TEXTILE;
	}

	@Override
	public MarkupLanguage getMarkupLanguage() {
		return lang;
	}

	@Override
	public List<String> getExtensions() {
		return settings.getStrings(Keys.web.textileExtensions);
	}

}
