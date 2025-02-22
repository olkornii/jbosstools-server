/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.as.ui.bot.itests.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.util.List;

import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.ui.PlatformUI;
import org.jboss.ide.eclipse.as.reddeer.server.view.JBossServer;
import org.jboss.tools.as.ui.bot.itests.Activator;
import org.jboss.tools.as.ui.bot.itests.download.RuntimeDownloadTestUtility;
import org.jboss.tools.as.ui.bot.itests.parametized.server.ServerRuntimeUIConstants;
import org.jboss.tools.as.ui.bot.itests.reddeer.util.DeployJSPProjectTemplate;
import org.jboss.tools.as.ui.bot.itests.reddeer.util.DetectRuntimeTemplate;
import org.jboss.tools.as.ui.bot.itests.reddeer.util.DisableSecureStorageRequirement.DisableSecureStorage;
import org.jboss.tools.as.ui.bot.itests.reddeer.util.OperateServerTemplate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * This test is trying to optimize and clean up the huge number of tests that
 * were here.
 * 
 * Pre-reqs: 1)
 * -Djbosstools.test.single.runtime.location=/path/to/some/wf/or/eap/unzipped/loc
 */

@RunWith(RedDeerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // first acquireAndDetect, then detect, then operate
@DisableSecureStorage
public class SingleServerRuntimeTest {

	private String location;
	private String serverName;
	private String serverIdentification;

	public SingleServerRuntimeTest() {
		location = System.getProperty("jbosstools.test.single.runtime.location");
		serverIdentification = System.getProperty("jbosstools.test.single.runtime.server.identification");
	}

	protected File getDownloadPath() {
		return new File(location);
	}

	@Test
	public void detect() {
		checkCountAndGetServerName();
	}

	private void checkCountAndGetServerName() {
		DetectRuntimeTemplate.detectRuntime(getDownloadPath().getAbsolutePath());
		DetectRuntimeTemplate.removePath(getDownloadPath().getAbsolutePath());
		ServersView2 serversView = new ServersView2();
		serversView.open();
		List<Server> all = serversView.getServers();
		assertThat(all.size(), is(1));
		serverName = all.get(0).getLabel().getName();
	}

	@Test
	public void operate() {
		checkCountAndGetServerName();
		OperateServerTemplate operate = new OperateServerTemplate(serverName);
		operate.setUp();
		try {
			operate.operateServer();
		} finally {
			operate.cleanServerAndConsoleView();
		}
	}

	@Test
	public void operateDeploy() {
		// Right now there's only 2 versions of each, and one is only for very old app
		// servers
		// So we'll use the JB7+ / WF / EAP5+ strings
		String undepString = ServerRuntimeUIConstants.getUndeployString(serverIdentification,
				"jsp-project", ".war");
		String depString = ServerRuntimeUIConstants.getDeployString(serverIdentification, "jsp-project",
				".war");

		checkCountAndGetServerName();
		OperateServerTemplate operate = new OperateServerTemplate(serverName);
		operate.setUp();
		operate.startServerSafe();
		DeployJSPProjectTemplate djsppt = new DeployJSPProjectTemplate();
		djsppt.clearConsole();
		JBossServer jbs = djsppt.getServer(serverName);
		djsppt.importProject("jsp-project", "projects/jsp-project.zip", serverName + " Runtime");

		djsppt.deployProject("jsp-project", serverName, depString);

		// Now try a hot-deploy
		djsppt.hotDeployment("jsp-project");

		// Now try to undeploy
		djsppt.undeployProject(serverName, "jsp-project", undepString);
	}

	@After
	public void postTest() {
		Display.syncExec(new Runnable() {
			@Override
			public void run() {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(true);
			}
		});
		new RuntimeDownloadTestUtility(getDownloadPath()).clean(false);
		OperateServerTemplate operate = new OperateServerTemplate(serverName);
		operate.cleanServerAndConsoleView();
	}

	@AfterClass
	public static void postClass() {
		new RuntimeDownloadTestUtility(Activator.getStateFolder().toFile()).clean(true);
	}

	@BeforeClass
	public static void beforeClass() {
		ServersView2 serversView = new ServersView2();
		serversView.open();
		serversView.getServers();
		List<Server> all = serversView.getServers();
		for (Server server : all) {
			server.delete();
		}
		new RuntimeDownloadTestUtility(Activator.getStateFolder().toFile()).clean(true);
	}

}
