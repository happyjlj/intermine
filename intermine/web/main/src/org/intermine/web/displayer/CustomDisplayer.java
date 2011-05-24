package org.intermine.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Parent class for custom displayers that appear on report pages.  Subclasses must implement the
 * display() method to place view information on the request.  CustomDisplayers are constructed
 * once and the display() method called many times so caching and one time setup can be performed in
 * the displayer class.
 * @author Richard Smith
 *
 */
public abstract class CustomDisplayer
{

    protected ReportDisplayerConfig config;
    protected InterMineAPI im;
    private static final Logger LOG = Logger.getLogger(CustomDisplayer.class);


    /**
     * Construct with config information read from webconfig-model.xml and the API.
     * @param config config information
     * @param im the InterMine API
     */
    public CustomDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        this.config = config;
        this.im = im;
    }

    /**
     * Execute is called for each report page with the object to be displayed.  This puts the
     * ReportObject and the JSP name to use on the request then calls the specific subclass'
     * display() method.
     * @param request request for displaying a report page
     * @param reportObject the object being displayed
     */
    public void execute(HttpServletRequest request, ReportObject reportObject) {
        request.setAttribute("reportObject", reportObject);
        request.setAttribute("jspPage", getJspPage());

        try {
            display(request, reportObject);
        } catch (Exception e) {
            // failed to display so put an error message in place instead
            LOG.error("Error rendering custom displayer " + getClass() + " for "
                    + reportObject.getType() + "(" + reportObject.getId() + "): "
                    + e.fillInStackTrace());
            request.setAttribute("displayerName", getClass().getSimpleName());
            request.setAttribute("jspPage", "customDisplayerError.jsp");

            Profile profile = SessionMethods.getProfile(request.getSession());
            if (profile.isSuperuser()) {
                request.setAttribute("exception",
                        ExceptionUtils.getStackTrace(ExceptionUtils.getRootCause(e)));
            }
        }
    }

    /**
     * To be implemented in subclasses where any specific information to be displayed should be
     * put on the request.
     * @param request request for displaying a report page
     * @param reportObject the object being displayed
     */
    public abstract void display(HttpServletRequest request, ReportObject reportObject);

    /**
     * The JSP page that will be called with the request to render output.
     * @return the JSP page name
     */
    public String getJspPage() {
        return config.getJspName();
    }

    /**
     * Get a list of field names and paths that should be replaced by this displayer - i.e. this
     * displayer should be shown instead of other fields.  The field names can include paths, e.g.
     * 'name' or 'department.name'.
     * @return the field names to replace
     */
    public Set<String> getReplacedFieldExprs() {
        return config.getReplacedFieldNames();
    }
}
