/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 19.12.2008 13:01:56
 *
 * $Id$
 */
package com.haulmont.cuba.core.sys;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.PersistenceProvider;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.SoftDelete;
import com.haulmont.cuba.core.entity.Updatable;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.openjpa.persistence.FetchPlan;

import java.util.*;

public class ViewHelper
{
    private static Log log = LogFactory.getLog(ViewHelper.class);

    public static void setView(FetchPlan fetchPlan, View view) {
        if (fetchPlan == null)
            throw new IllegalArgumentException("FetchPlan is null");

        fetchPlan.clearFetchGroups();

        if (view != null) {
            fetchPlan.removeFetchGroup(FetchPlan.GROUP_DEFAULT);
            fetchPlan.setExtendedPathLookup(true);
            processView(view, fetchPlan);
        } else {
            fetchPlan.addFetchGroup(FetchPlan.GROUP_DEFAULT);
        }
    }

    public static void addView(FetchPlan fetchPlan, View view) {
        if (fetchPlan == null)
            throw new IllegalArgumentException("FetchPlan is null");
        if (view == null)
            throw new IllegalArgumentException("View is null");

        processView(view, fetchPlan);
    }

    public static View intersectViews(View first, View second) {
        if (first == null)
            throw new IllegalArgumentException("View is null");
        if (second == null)
            throw new IllegalArgumentException("View is null");

        View resultView = new View(first.getEntityClass());

        Collection<ViewProperty> firstProps = first.getProperties();

        for (ViewProperty firstProperty : firstProps) {
            if (second.containsProperty(firstProperty.getName())) {
                View resultPropView = null;
                ViewProperty secondProperty = second.getProperty(firstProperty.getName());
                if ((firstProperty.getView() != null) && (secondProperty.getView() != null)) {
                    resultPropView = intersectViews(firstProperty.getView(), secondProperty.getView());
                }
                resultView.addProperty(firstProperty.getName(), resultPropView);
            }
        }

        return resultView;
    }

    private static void processView(View view, FetchPlan fetchPlan) {
        if (view.isIncludeSystemProperties()) {
            includeSystemProperties(view, fetchPlan);
        }

        for (ViewProperty property : view.getProperties()) {
            if (property.isLazy())
                continue;

            fetchPlan.addField(view.getEntityClass(), property.getName());
            if (property.getView() != null) {
                processView(property.getView(), fetchPlan);
            }
        }
    }

    private static void includeSystemProperties(View view, FetchPlan fetchPlan) {
        Class<? extends BaseEntity> entityClass = view.getEntityClass();
        if (BaseEntity.class.isAssignableFrom(entityClass)) {
            for (String property : BaseEntity.PROPERTIES) {
                fetchPlan.addField(entityClass, property);
            }
        }
        if (Updatable.class.isAssignableFrom(entityClass)) {
            for (String property : Updatable.PROPERTIES) {
                fetchPlan.addField(entityClass, property);
            }
        }
        if (SoftDelete.class.isAssignableFrom(entityClass)) {
            for (String property : SoftDelete.PROPERTIES) {
                fetchPlan.addField(entityClass, property);
            }
        }
    }

    public static void fetchInstance(Instance instance, View view) {
        if (PersistenceHelper.isDetached(instance))
            throw new IllegalArgumentException("Can not fetch detached entity. Merge first.");
        __fetchInstance(instance, view, new HashMap<Instance, Set<View>>());
    }

    private static void __fetchInstance(Instance instance, View view, Map<Instance, Set<View>> visited) {
        Set<View> views = visited.get(instance);

        if (views == null) {
            views = new HashSet<View>();
            visited.put(instance, views);
        } else if (views.contains(view)) {
            return;
        }

        views.add(view);

        if (log.isTraceEnabled()) log.trace("Fetching instance " + instance);
        for (ViewProperty property : view.getProperties()) {
            if (log.isTraceEnabled()) log.trace("Fetching property " + property.getName());
            Object value = instance.getValue(property.getName());
            View propertyView = property.getView();
            if (value != null && propertyView != null) {
                if (value instanceof Collection) {
                    for (Object item : ((Collection) value)) {
                        if (item instanceof Instance)
                            __fetchInstance((Instance) item, propertyView, visited);
                    }
                } else if (value instanceof Instance) {
                    if (PersistenceHelper.isDetached(value)) {
                        log.trace("Object " + value + " is detached, loading it");
                        EntityManager em = PersistenceProvider.getEntityManager();
                        Entity entity = (Entity) value;
                        value = em.find(entity.getClass(), entity.getId());
                        if (value == null) {
                            // the instance is most probably deleted
                            continue;
                        }
                        instance.setValue(property.getName(), value);
                    }
                    __fetchInstance((Instance) value, propertyView, visited);
                }
            }
        }
    }

    public static boolean hasLazyProperties(View view) {
        for (ViewProperty property : view.getProperties()) {
            if (property.isLazy())
                return true;
            if (property.getView() != null) {
                if (hasLazyProperties(property.getView()))
                    return true;
            }
        }
        return false;
    }
}
