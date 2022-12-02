/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.ide.eclipse.as.core.server.internal.extendedproperties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;

public class Wildfly24PlusExtendedProperties extends Wildfly15PlusExtendedProperties {
    public Wildfly24PlusExtendedProperties(IAdaptable obj, String rtVersionString) {
        super(obj, rtVersionString);
    }

    @Override
    public IExecutionEnvironment getDefaultExecutionEnvironment() {
        return JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("JavaSE-11"); //$NON-NLS-1$
    }
}
