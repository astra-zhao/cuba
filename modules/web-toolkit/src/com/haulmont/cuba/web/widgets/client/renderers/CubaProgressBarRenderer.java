/*
 * Copyright (c) 2008-2017 Haulmont.
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

package com.haulmont.cuba.web.widgets.client.renderers;

import com.google.gwt.core.shared.GWT;
import com.haulmont.cuba.web.widgets.client.renderers.widgets.progressbar.CubaProgressBarWidget;
import com.vaadin.client.renderers.ProgressBarRenderer;

public class CubaProgressBarRenderer extends ProgressBarRenderer {

    @Override
    public CubaProgressBarWidget createWidget() {
        CubaProgressBarWidget progressBar = GWT.create(CubaProgressBarWidget.class);
        progressBar.addStyleDependentName("static");
        progressBar.setClickThroughEnabled(true);
        return progressBar;
    }
}