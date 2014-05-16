package com.gitblit.markup;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.utils.StringUtils;
import com.gitblit.wicket.GitBlitWebApp;
import com.gitblit.wicket.WicketUtils;

public abstract class AbstractMarkupSyntax implements MarkupParser {
	protected IStoredSettings settings;
	protected GitBlitWebApp app;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public AbstractMarkupSyntax(GitBlitWebApp app) {
		this.app=app;
		this.settings=app.settings();
	}
	@Override
	public boolean canHandle(String documentExt, String documentPath) {
		return getExtensions().contains(documentExt);
	}
	
	protected String getWicketUrl(Class<? extends Page> pageClass, final String repositoryName, final String commitId, final String document) {
		String fsc = settings.getString(Keys.web.forwardSlashCharacter, "/");
		String encodedPath = document.replace(' ', '-');
		try {
			encodedPath = URLEncoder.encode(encodedPath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.error(null, e);
		}
		encodedPath = encodedPath.replace("/", fsc).replace("%2F", fsc);

		String url = RequestCycle.get().urlFor(pageClass, WicketUtils.newPathParameter(repositoryName, commitId, encodedPath)).toString();
		return url;
	}
	
	protected String getDocumentName(final String document) {
		// extract document name
		String name = StringUtils.stripFileExtension(document);
		name = name.replace('_', ' ');
		if (name.indexOf('/') > -1) {
			name = name.substring(name.lastIndexOf('/') + 1);
		}
		return name;
	}
}
