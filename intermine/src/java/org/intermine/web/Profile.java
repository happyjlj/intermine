package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/**
 * Class to represent a user of the webapp
 * @author Mark Woodbridge
 */
public class Profile
{
    protected ProfileManager manager;
    protected String username;
    protected Map savedQueries = new LinkedHashMap();
    protected Map savedBags = new LinkedHashMap();
    protected Map savedTemplates = new LinkedHashMap();

    /**
     * Construct a Profile
     * @param manager the manager for this profile
     * @param username the username for this profile
     * @param savedQueries the saved queries for this profile
     * @param savedBags the saved bags for this profile
     * @param savedTemplates the saved templates for this profile
     */
    public Profile(ProfileManager manager,
                   String username,
                   Map savedQueries,
                   Map savedBags,
                   Map savedTemplates) {
        this.manager = manager;
        this.username = username;
        this.savedQueries.putAll(savedQueries);
        this.savedBags.putAll(savedBags);
        this.savedTemplates.putAll(savedTemplates);
    }
    
    /**
     * Get the value of username
     * @return the value of username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Get the users saved templates
     * @return saved templates
     */
    public Map getSavedTemplates() {
        return Collections.unmodifiableMap(savedTemplates);
    }

    /**
     * Save a template
     * @param name the template name
     * @param template the template
     */
    public void saveTemplate(String name, TemplateQuery template) {
        savedTemplates.put(name, template);
        if (manager != null) {
            manager.saveProfile(this);
        }
    }
    
    /**
     * Delete a template
     * @param name the template name
     */
    public void deleteTemplate(String name) {
        savedTemplates.remove(name);
        if (manager != null) {
            manager.saveProfile(this);
        }
    }

    /**
     * Get the value of savedQueries
     * @return the value of savedQueries
     */
    public Map getSavedQueries() {
        return Collections.unmodifiableMap(savedQueries);
    }

    /**
     * Save a query
     * @param name the query name
     * @param query the query
     */
    public void saveQuery(String name, PathQuery query) {
        savedQueries.put(name, query);
        if (manager != null) {
            manager.saveProfile(this);
        }
    }

    /**
     * Delete a query
     * @param name the query name
     */
    public void deleteQuery(String name) {
        savedQueries.remove(name);
        if (manager != null) {
            manager.saveProfile(this);
        }
    }

    /**
     * Get the value of savedBags
     * @return the value of savedBags
     */
    public Map getSavedBags() {
        return Collections.unmodifiableMap(savedBags);
    }

    /**
     * Save a bag
     * @param name the bag name
     * @param bag the bag
     */
    public void saveBag(String name, InterMineBag bag) {
        savedBags.put(name, bag);
        if (manager != null) {
            manager.saveProfile(this);
        }
    }

    /**
     * Delete a bag
     * @param name the bag name
     */
    public void deleteBag(String name) {
        savedBags.remove(name);
        if (manager != null) {
            manager.saveProfile(this);
        }
    }
}