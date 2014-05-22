/*
 * Copyright 2012 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.Constants;
import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.dagger.DaggerServlet;
import com.gitblit.markup.DefaultPagesController;
import com.gitblit.markup.PagesController;

import dagger.ObjectGraph;

/**
 * Serves the content of a gh-pages branch.
 * 
 * @author James Moger
 * 
 */
public class PagesServlet extends DaggerServlet {

	private static final long serialVersionUID = 1L;

	private transient Logger logger = LoggerFactory.getLogger(PagesServlet.class);
	private transient PagesController controller;

	private IStoredSettings settings;

	@Override
	protected void inject(ObjectGraph dagger) {
		this.settings = dagger.get(IStoredSettings.class);
		try {
			String controllerClazz=settings.getString(Keys.web._ROOT + ".pages.controller", DefaultPagesController.class.getName());
			logger.info("controller class:"+controllerClazz);
			this.controller = (PagesController) Class.forName(controllerClazz).newInstance();
			this.controller.init(dagger);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns an url to this servlet for the specified parameters.
	 * 
	 * @param baseURL
	 * @param repository
	 * @param path
	 * @return an url
	 */
	public static String asLink(String baseURL, String repository, String path) {
		if (baseURL.length() > 0 && baseURL.charAt(baseURL.length() - 1) == '/') {
			baseURL = baseURL.substring(0, baseURL.length() - 1);
		}
		return baseURL + Constants.PAGES + repository + "/" + (path == null ? "" : ("/" + path));
	}

	/**
	 * Retrieves the specified resource from the gh-pages branch of the repository.
	 * 
	 * @param request
	 * @param response
	 * @throws javax.servlet.ServletException
	 * @throws java.io.IOException
	 */
	private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		controller.processRequest(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}
}
