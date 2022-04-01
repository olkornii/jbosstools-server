/******************************************************************************* 
 * Copyright (c) 2018-2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.as.jmx.ui.bot.itests.server.connection;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.AbstractWait;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.direct.preferences.Preferences;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewEnums.ServerState;
import org.eclipse.reddeer.eclipse.condition.ServerHasState;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewException;
import org.eclipse.reddeer.junit.annotation.RequirementRestriction;
import org.eclipse.reddeer.junit.requirement.matcher.RequirementMatcher;
import org.eclipse.reddeer.requirements.server.ServerRequirementState;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.swt.widgets.Shell;
import org.hamcrest.core.IsNull;
import org.jboss.ide.eclipse.as.reddeer.server.requirement.ServerRequirement.JBossServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class covering Remote server JMX connection functionality
 * 
 * @author odockal
 *
 */
@JBossServer(cleanup = true, state = ServerRequirementState.PRESENT)
public class RemoteServerJMXConnectionTest extends JMXServerTestTemplate {

	private static final Logger log = Logger.getLogger(RemoteServerJMXConnectionTest.class);

	@RequirementRestriction
	public static Collection<RequirementMatcher> getRestrictionMatcher() {
		return Arrays.asList(new RequirementMatcher(JBossServer.class, "remote", IsNull.notNullValue()));
	}

	@Before
	public void setupServer() {
		Preferences.set("org.eclipse.debug.ui", "Console.limitConsoleOutput", "false");
		setUpView();

		server = getServer(serverConfig.getServerName());
		serverItem = view.getServerConnectionsItem();
	}

	@After
	public void tearDownServer() {
		try {
			stopServer();
		} catch (ServersViewException serverExc) {
			log.error(serverExc.getMessage());
			if (!server.getLabel().getState().equals(ServerState.STOPPED)) {
				throw serverExc;
			}
		} catch (WaitTimeoutExpiredException exc) {
			log.error(exc.getMessage() + ", server is not stopping due to JBIDE-26311");
			// workaround for JBIDE-26311
			ShellIsAvailable shell = new ShellIsAvailable("Terminate Server");
			new WaitUntil(shell, false);
			if (shell.getResult() != null) {
				processShell(shell.getResult());
			}
			ShellIsAvailable shell2 = new ShellIsAvailable("Problem Occured");
			new WaitUntil(shell2, false);
			if (shell2.getResult() != null) {
				processShell(shell2.getResult());
			}
			if (!server.getLabel().getState().equals(ServerState.STOPPED)) {
				throw exc;
			}
		}
	}

	private void processShell(Shell shell) {
		DefaultShell dialog = new DefaultShell(shell);
		new PushButton("OK").click();
		new WaitWhile(new ShellIsAvailable(dialog), false);
	}

	@Test
	public void testJMXServerConnection() {
		verifyJMXServerConnection(serverConfig.getServerName());
	}

	@Test
	public void testServerOperation() {
		startServer();
		getServerJMXConnection();
		connection.connect();

		verifyServerOperation(verifyRunScanOperation(openOperationsPage(connection)));
	}
	
    public void stopServer() {
    	new ServersView2().activate();
    	server.select();
		try {
			new ContextMenuItem("Stop").select();
			new WaitWhile(new JobIsRunning());
			new WaitUntil(new ServerHasState(server, ServerState.STOPPED), TimePeriod.LONG);
		} catch (WaitTimeoutExpiredException ex){
			//try it once again
			new ContextMenuItem("Stop").select();
			new WaitWhile(new JobIsRunning());
			new WaitUntil(new ServerHasState(server, ServerState.STOPPED), TimePeriod.LONG);
		}
    }

}
