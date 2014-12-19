package org.intermine.web.search;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;

/**
 * Container for a single result row from the keyword search
 * @author nils
 */
public class KeywordSearchResult
{
    private static final Logger LOG = Logger.getLogger(KeywordSearchResult.class);

    final WebConfig webconfig;
    final InterMineObject object;

    final int id;
    final List<String> types = new ArrayList<String>();
    final float score;
    final Map<String, TemplateQuery> templates;

    final int points;
    final Map<String, FieldConfig> fieldConfigs;
    final List<String> keyFields;
    final List<String> additionalFields;
    final Map<String, Object> fieldValues;
    String linkRedirect = null;

    /**
     * create the container object - automatically reads fields and saves the results in members
     * @param webconfig webconfig
     * @param object the object this result should contain
     * @param classKeys keys associated with this class
     * @param classDescriptors descriptors for this class
     * @param score score for this hit
     * @param templates templatequeries for this class
     * @param linkRedirect URL that search result will link to, if not report page
     */
    public KeywordSearchResult(WebConfig webconfig, InterMineObject object,
            Map<String, List<FieldDescriptor>> classKeys,
            Collection<ClassDescriptor> classDescriptors,
            float score, Map<String, TemplateQuery> templates, String linkRedirect) {

        this.fieldConfigs = new HashMap<String, FieldConfig>();
        this.keyFields = new ArrayList<String>();
        this.additionalFields = new ArrayList<String>();
        this.fieldValues = new HashMap<String, Object>();

        for (ClassDescriptor cld: classDescriptors) {
            types.add(cld.getUnqualifiedName());
            List<FieldConfig> fieldConfigList =
                    FieldConfigHelper.getClassFieldConfigs(webconfig, cld);
            for (FieldConfig fieldConfig : fieldConfigList) {
                if (fieldConfig.getShowInSummary()) {
                    String path = cld.getUnqualifiedName() + "." + fieldConfig.getFieldExpr();
                    fieldConfigs.put(path, fieldConfig);

                    if (ClassKeyHelper.isKeyField(classKeys, cld.getName(),
                            fieldConfig.getFieldExpr())) {
                        this.keyFields.add(path);
                    } else {
                        this.additionalFields.add(path);
                    }

                    if (fieldConfig.getDisplayer() == null) {
                        Object value = getValueForField(object, fieldConfig.getFieldExpr());
                        if (value != null) {
                            fieldValues.put(path, value);
                        }
                    }
                }
            }
        }

        this.webconfig = webconfig;
        this.object = object;
        this.id = object.getId();
        this.score = score;
        this.templates = templates;
        this.points = Math.round(Math.max(0.1F, Math.min(1, getScore())) * 10); // range 1..10
        this.linkRedirect = linkRedirect;
    }

    private Object getValueForField(InterMineObject object, String expression) {
        LOG.debug("Getting field " + object.getClass().getName() + " -> " + expression);
        Object value = null;

        try {
            int dot = expression.indexOf('.');
            if (dot > -1) {
                String subExpression = expression.substring(dot + 1);
                Object reference = object.getFieldValue(expression.substring(0, dot));
                LOG.debug("Reference=" + reference);

                // recurse into next object
                if (reference != null) {
                    if (reference instanceof InterMineObject) {
                        value = getValueForField((InterMineObject) reference, subExpression);
                    } else {
                        LOG.warn("Reference is not an IMO in " + object.getClass().getName()
                                + " -> " + expression);
                    }
                }
            } else {
                value = object.getFieldValue(expression);
            }
        } catch (Exception e) {
            LOG.error("Value/reference not found", e);
        }
        return value;
    }

    /**
     * returns original intermine object
     * @return object
     */
    public InterMineObject getObject() {
        return object;
    }

    /**
     * intermine ID
     * @return x
     */
    public int getId() {
        return id;
    }

    /**
     * returns the name of the class for this object (category)
     * @return type
     */
    public Collection<String> getTypes() {
        return types;
    }

    /**
     * return score
     * @return ...
     */
    public float getScore() {
        return score;
    }

    /**
     * templates associated with this class
     * @return map of internal template name to template query
     */
    public Map<String, TemplateQuery> getTemplates() {
        return templates;
    }

    /**
     * return points
     * @return 1..10
     */
    public int getPoints() {
        return points;
    }

    /**
     * URL set in web.properties.
     *
     * @return the URL the search result will link to. if NULL, link to report page
     */
    public String getLinkRedirect() {
        return linkRedirect;
    }

    /**
     * fieldConfigs
     * @return map from field expression to fieldConfigs
     */
    public Map<String, FieldConfig> getFieldConfigs() {
        return fieldConfigs;
    }

    /**
     * key paths
     * @return keyFields
     */
    public final Collection<String> getKeyFields() {
        return keyFields;
    }

    /**
     * additional display paths
     * @return additionalFields
     */
    public final Collection<String> getAdditionalFields() {
        return additionalFields;
    }

    /**
     * values of all fields
     * @return map from path to value
     */
    public Map<String, Object> getFieldValues() {
        return fieldValues;
    }

    /**
     * Return the field values, with the class names removed.
     * @return A map from fieldExpression to value.
     */
    public Map<String, Object> getHeadlessFieldValues() {
        Map<String, Object> ret = new HashMap<String, Object>();
        for (String path: fieldValues.keySet()) {
            String headless = path.replaceAll("^\\w+.", "");
            ret.put(headless, fieldValues.get(path));
        }
        return ret;
    }

}
