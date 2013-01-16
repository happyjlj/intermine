package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Hold data about primary identifiers and synonyms for a particular class in the
 * data model and provide methods to resolved synonyms into corresponding
 * primary identifier(s).
 *
 * @author rns
 * @author Fengyuan Hu
 */
public class IdResolver
{
    private static final Logger LOG = Logger.getLogger(IdResolver.class);

    private String clsName;

    @SuppressWarnings("unchecked")
    protected Map<MultiKey, Map<String, Set<String>>> orgIdMaps = new MultiKeyMap();
    @SuppressWarnings("unchecked")
    protected Map<MultiKey, Map<String, Set<String>>> orgSynMaps = new MultiKeyMap();
    @SuppressWarnings("unchecked")
    protected Map<MultiKey, Map<String, Set<String>>> orgMainMaps = new MultiKeyMap();
    @SuppressWarnings("unchecked")
    protected Map<MultiKey, Map<String, Set<String>>> orgIdMainMaps = new MultiKeyMap();
    @SuppressWarnings("unchecked")
    protected Map<MultiKey, Map<String, Set<String>>> orgIdSynMaps = new MultiKeyMap();

    /**
     * Construct and empty IdResolver
     */
    public IdResolver() {
    }

    /**
     * Construct and empty IdResolver
     * @param clsName the class to resolve identifiers for
     */
    public IdResolver(String clsName) {
        this.clsName = clsName;
    }

    // check that the given taxon id has some data for it
    // if an exception thrown, there must be something wrong with resolver factory.
    protected void checkTaxonId(String taxonId, String clsName) {
        if (!orgIdMaps.containsKey(new MultiKey(taxonId, clsName))) {
            throw new IllegalArgumentException(clsName + " IdResolver has "
                                               + "no data for taxonId: "
                                               + taxonId + ".");
        }
    }

    /**
     * Check whether the given id is a primary identifier for this taxonId
     * @param taxonId the organism to look up
     * @param clsName go term
     * @param id an identifier
     * @return true if id is a primaryIdentifier
     */
    public boolean isPrimaryIdentifier(String taxonId, String clsName, String id) {
        checkTaxonId(taxonId, clsName);
        return orgIdMaps.get(new MultiKey(taxonId, clsName)).containsKey(id);
    }

    /**
     * Check whether the given id is a primary identifier for this taxonId
     * @param taxonId the organism to look up
     * @param id an identifier
     * @return true if id is a primaryIdentifier
     */
    public boolean isPrimaryIdentifier(String taxonId, String id) {
        return isPrimaryIdentifier(taxonId, this.clsName, id);
    }

    /**
     * For the given id return a set of matching primary identifiers in the given
     * taxonId.  In many cases the set will have just one element. Some will have
     * zero element.
     * @param taxonId the organism to search within
     * @param clsName go term
     * @param id the identifier to resolve
     * @return a set of matching primary identifiers
     */
    public Set<String> resolveId(String taxonId, String clsName, String id) {
        checkTaxonId(taxonId,clsName);
        // if this is a primary identifier, just return it
        if (isPrimaryIdentifier(taxonId, clsName, id)) {
            return Collections.singleton(id);
        }
        if (orgMainMaps.containsKey(new MultiKey(taxonId, clsName))
            && orgMainMaps.get(new MultiKey(taxonId, clsName)).containsKey(id)) {
            return orgMainMaps.get(new MultiKey(taxonId, clsName)).get(id);
        }
        if (orgSynMaps.containsKey(new MultiKey(taxonId, clsName))
            && orgSynMaps.get(new MultiKey(taxonId, clsName)).containsKey(id)) {
            return orgSynMaps.get(new MultiKey(taxonId, clsName)).get(id);
        }
        return Collections.emptySet();
    }

    /**
     * For the given set of ids return a map of matching primary identifiers in the given
     * taxonId.  In many cases the set will have just one element. Some will have
     * zero element.
     * @param taxonId the organism to search within
     * @param clsName go term
     * @param ids the identifier set to resolve
     * @return a set of common pid
     */
    public Set<String> resolveIds(String taxonId, String clsName, List<String> ids) {
        Set<String> common = new LinkedHashSet<String>();
        for (int i=0; i<ids.size();i++) {
            Set<String> resovledSet = resolveId(taxonId, clsName, ids.get(i));
            if (i == 0) {
                common.addAll(resovledSet);
            } else {
                common.retainAll(resovledSet);
            }
        }

        return common;
    }

    /**
     * For the given id return a set of matching primary identifiers in the given
     * taxonId.  In many cases the set will have just one element. Some will have
     * zero element.
     * @param taxonId the organism to search within
     * @param id the identifier to resolve
     * @return a set of matching primary identifiers
     */
    public Set<String> resolveId(String taxonId, String id) {
        return resolveId(taxonId, this.clsName, id);
    }

    /**
     * For the given id set return a map of matching primary identifiers in the given
     * taxonId.  In many cases the set will have just one element. Some will have
     * zero element.
     * @param taxonId the organism to search within
     * @param ids the identifier set to resolve
     * @return a map of matching primary identifiers
     */
    public Set<String> resolveIds(String taxonId, List<String> ids) {
        return resolveIds(taxonId, this.clsName, ids);
    }

    /**
     * For a particular primary identifier fetch a set of synonyms or return
     * null if id is not a primary identifier for the taxonId given.
     * @param taxonId the organism to do a lookup for
     * @param clsName go term
     * @param id the primary identifier to look up
     * @return a set of synonyms or null if id is not a primary identifier
     */
    public Set<String> getSynonyms(String taxonId, String clsName, String primaryIdentifier) {
        checkTaxonId(taxonId, clsName);
        if (!isPrimaryIdentifier(taxonId, clsName, primaryIdentifier)) {
            return null;
        }
        return orgIdMaps.get(new MultiKey(taxonId, clsName)).get(primaryIdentifier);
    }

    /**
     * For a particular primary identifier fetch a set of synonyms or return
     * null if id is not a primary identifier for the taxonId given.
     * @param taxonId the organism to do a lookup for
     * @param id the primary identifier to look up
     * @return a set of synonyms or null if id is not a primary identifier
     */
    public Set<String> getSynonyms(String taxonId, String primaryIdentifier) {
        return getSynonyms(taxonId, this.clsName, primaryIdentifier);
    }

    /**
     * Return the count of matching primary identifiers for a particular identifier
     * @param taxonId the organism to check for
     * @param clsName go term
     * @param id the identifier to look up
     * @return a count of the resolutions for this identifier
     */
    public int countResolutions(String taxonId, String clsName, String id) {
        checkTaxonId(taxonId, clsName);
        Set<String> resolvedIds = resolveId(taxonId, clsName, id);
        return resolvedIds == null ? 0 : resolvedIds.size();
    }

    /**
     * Return the count of matching primary identifiers for a particular identifier
     * @param taxonId the organism to check for
     * @param id the identifier to look up
     * @return a count of the resolutions for this identifier
     */
    public int countResolutions(String taxonId, String id) {
        return countResolutions(taxonId, this.clsName, id);
    }

    /**
     * Return true if the idResolver contains information about this taxon id.
     * @param taxonId an organism to check for
     * @return true if data about this taxon id
     */
    public boolean hasTaxon(String taxonId) {
        return hasTaxons(new HashSet<String>(Arrays.asList(new String[]{taxonId})));
    }

    /**
     * Return true if the idResolver contains information about a collection of taxon id.
     * @param taxonIds a collection of organism to check for
     * @return true if data about this taxon id
     */
    public boolean hasTaxons(Set<String> taxonIds) {
        Set<String> taxonIdSet = new HashSet<String>();
        for (MultiKey key : orgIdMaps.keySet()) {
            taxonIdSet.add((String) key.getKey(0));
        }
        return taxonIdSet.containsAll(taxonIds);
    }

    /**
     * Return a set of taxon id.
     * @return all taxon ids in resolver
     */
    public Set<String> getTaxons() {
        Set<String> taxonIdSet = new HashSet<String>();
        for (MultiKey key : orgIdMaps.keySet()) {
            taxonIdSet.add((String) key.getKey(0));
        }
        return taxonIdSet;
    }

    /**
     * Return true if the idResolver contains information about this class name.
     * @param clsName an go term to check for
     * @return true if has this term
     */
    public boolean hasClassName(String clsName) {
        Set<String> clsNameSet = new HashSet<String>();
        for (MultiKey key : orgIdMaps.keySet()) {
            clsNameSet.add((String) key.getKey(1));
        }
        return clsNameSet.contains(clsName);
    }

    /**
     * Return a set of class names the reslover holds
     * @return a set of class names
     */
    public Set<String> getClassNames() {
        Set<String> clsNameSet = new HashSet<String>();
        for (MultiKey key : orgIdMaps.keySet()) {
            clsNameSet.add((String) key.getKey(1));
        }
        return clsNameSet;
    }

    /**
     * Check if resolver has taxon id and class name
     * @param taxonId taxon id as string
     * @param clsName class name as string
     */
    public boolean hasTaxonAndClassName(String taxonId, String clsName) {
        return orgIdMaps.keySet().contains(new MultiKey(taxonId, clsName));
    }

    /**
     * Check if resolver has taxon id and class name
     * @param taxonId taxon id as string
     * @param clsName class name as string
     */
    public boolean hasTaxonAndClassNames(String taxonId, Set<String> clsNames) {
        Map<String, Set<String>> taxonIdAndClsNameMap = new HashMap<String, Set<String>>();
        taxonIdAndClsNameMap.put(taxonId, clsNames);
        return hasTaxonsAndClassNames(taxonIdAndClsNameMap);
    }

    /**
     * Check if resolver has taxon id and class name
     * @param taxonId taxon id as string
     * @param clsName class name as string
     */
    public boolean hasTaxonsAndClassName(Set<String> taxonIds, String clsName) {
        Map<String, Set<String>> taxonIdAndClsNameMap = new HashMap<String, Set<String>>();
        for (String taxonId : taxonIds) {
            taxonIdAndClsNameMap
                    .put(taxonId,
                            new HashSet<String>(Arrays
                                    .asList(new String[] { clsName })));
        }
        return hasTaxonsAndClassNames(taxonIdAndClsNameMap);
    }

    /**
     * Check if resolver has a set of keys (taxon id + class name)
     * @param taxonIdAndClsNameMap data structure to hold keys
     * @return boolean value
     */
    public boolean hasTaxonsAndClassNames(Map<String, Set<String>> taxonIdAndClsNameMap) {
        Set<MultiKey> keySet = new HashSet<MultiKey>();
        for (Entry<String, Set<String>> e : taxonIdAndClsNameMap.entrySet()) {
            for (String clsName : e.getValue()) {
                keySet.add(new MultiKey(e.getKey(), clsName));
            }
        }

        return orgIdMaps.keySet().containsAll(keySet);
    }

    /**
     * Get a set of keys (taxon id + class name) resolver holds
     * @return a set of MultiKey, parse it to use, e.g. Map<taxonid, Set<clsName>>
     */
    public Map<String, Set<String>> getTaxonsAndClassNames() {
        Map<String, Set<String>> taxonIdAndClsNameMap = new HashMap<String, Set<String>>();
        for (MultiKey key : orgIdMaps.keySet()) {
            String taxonId = (String) key.getKey(0);
            String clsName = (String) key.getKey(1);
            if (taxonIdAndClsNameMap.get(taxonId) == null) {
                taxonIdAndClsNameMap.put(
                        taxonId,
                        new HashSet<String>(Arrays
                                .asList(new String[] { clsName })));
            } else {
                taxonIdAndClsNameMap.get(taxonId).add(clsName);
            }
        }

        return taxonIdAndClsNameMap;
    }

    /**
     * Add alternative main identifiers for a primary identifier to the IdResolver.
     * @param taxonId the organism of the identifier
     * @param clsName go term
     * @param primaryIdentifier the main identifier
     * @param ids a set of alternative main identifiers
     */
    protected void addMainIds(String taxonId, String clsName, String primaryIdentifier,
            Set<String> ids) {
        addEntry(taxonId, clsName, primaryIdentifier, ids, Boolean.TRUE);
    }

    /**
     * Add alternative main identifiers for a primary identifier to the IdResolver.
     * @param taxonId the organism of the identifier
     * @param primaryIdentifier the main identifier
     * @param ids a set of alternative main identifiers
     */
    protected void addMainIds(String taxonId, String primaryIdentifier, Set<String> ids) {
        addMainIds(taxonId, this.clsName, primaryIdentifier, ids);
    }

    /**
     * Add synonyms for a primary identifier to the IdResolver
     * @param taxonId the organism of the identifier
     * @param clsName go term
     * @param primaryIdentifier the main identifier
     * @param ids a set synonyms
     */
    protected void addSynonyms(String taxonId, String clsName, String primaryIdentifier,
            Set<String> ids) {
        addEntry(taxonId, clsName, primaryIdentifier, ids, Boolean.FALSE);
    }

    /**
     * Add synonyms for a primary identifier to the IdResolver
     * @param taxonId the organism of the identifier
     * @param primaryIdentifier the main identifier
     * @param ids a set synonyms
     */
    protected void addSynonyms(String taxonId, String primaryIdentifier, Set<String> ids) {
        addSynonyms(taxonId, this.clsName, primaryIdentifier, ids);
    }

    /**
     * Create entries for the IdResolver, these will be added when getIdResolver
     * is called.
     * @param taxonId the organism of identifiers
     * @param clsName go term
     * @param primaryId main identifier
     * @param synonyms synonyms for the main identifier
     */
    public void addResolverEntry(String taxonId, String clsName,
            String primaryId, Set<String> synonyms) {
        addSynonyms(taxonId, clsName, primaryId, synonyms);
    }

    /**
     * Create entries for the IdResolver, these will be added when getIdResolver
     * is called.
     * @param taxonId the organism of identifiers
     * @param primaryId main identifier
     * @param synonyms synonyms for the main identifier
     */
    public void addResolverEntry(String taxonId, String primaryId, Set<String> synonyms) {
        addResolverEntry(taxonId, this.clsName, primaryId, synonyms);
    }

    /**
     * Add an entry to the IdResolver, a primary identifier and any number of synonyms.
     * @param taxonId the organism of the identifier
     * @param clsName go term
     * @param primaryIdentifier the main identifier
     * @param synonyms a set of synonyms
     * @param mainId if true these are main ids, otherwise synonms
    */
    protected void addEntry(String taxonId, String clsName, String primaryIdentifier,
            Collection<String> ids, Boolean mainId) {
        Map<String, Set<String>> idMap = orgIdMaps.get(new MultiKey(taxonId, clsName));
        if (idMap == null) {
            idMap = new LinkedHashMap<String, Set<String>>();
            orgIdMaps.put(new MultiKey(taxonId, clsName), idMap);
        }

        addToMapList(idMap, primaryIdentifier, ids);

        Map<String, Set<String>> lookupMap = null;
        Map<String, Set<String>> reverseMap = null;
        if (mainId.booleanValue()) {
            lookupMap = orgMainMaps.get(new MultiKey(taxonId, clsName));
            if (lookupMap == null) {
                lookupMap = new HashMap<String, Set<String>>();
                orgMainMaps.put(new MultiKey(taxonId, clsName), lookupMap);
            }

            reverseMap = orgIdMainMaps.get(new MultiKey(taxonId, clsName));
            if (reverseMap == null) {
                reverseMap = new LinkedHashMap<String, Set<String>>();
                orgIdMainMaps.put(new MultiKey(taxonId, clsName), reverseMap);
            }
        } else {
            // these ids are synonyms
            lookupMap = orgSynMaps.get(new MultiKey(taxonId, clsName));
            if (lookupMap == null) {
                lookupMap = new LinkedHashMap<String, Set<String>>();
                orgSynMaps.put(new MultiKey(taxonId, clsName), lookupMap);
            }

            reverseMap = orgIdSynMaps.get(new MultiKey(taxonId, clsName));
            if (reverseMap == null) {
                reverseMap = new LinkedHashMap<String, Set<String>>();
                orgIdSynMaps.put(new MultiKey(taxonId, clsName), reverseMap);
            }
        }

        // map from primaryId back to main/synonym ids
        addToMapList(reverseMap, primaryIdentifier, ids);

        for (String id : ids) {
            addToMapList(lookupMap, id, Collections.singleton(primaryIdentifier));
        }
    }

    /**
     * Write IdResolver contents to a flat file
     * @param f the file to write to
     * @throws IOException if fail to write
     */
    public void writeToFile(File f) throws IOException {
        LOG.info("Writing id resolver to file: " + f.getName());
        FileWriter fw = new FileWriter(f, true); // append if true
//        FileWriter fw = new FileWriter(f);
        for (MultiKey key : orgIdMaps.keySet()) {

            // get maps for this organism
            Map<String, Set<String>> idMap = orgIdMaps.get(key);
            Map<String, Set<String>> mainIdsMap = orgIdMainMaps.get(key);
            Map<String, Set<String>> synonymMap = orgIdSynMaps.get(key);

            for (Map.Entry<String, Set<String>> idMapEntry : idMap.entrySet()) {
                StringBuffer sb = new StringBuffer();

                String primaryId = idMapEntry.getKey();

                sb.append((String) key.getKey(0) + "\t");  // write taxon id
                sb.append((String) key.getKey(1) + "\t");  // write class name
                sb.append(primaryId + "\t");  // write primary id

                if (mainIdsMap != null && mainIdsMap.containsKey(primaryId)) {
                    boolean first = true;
                    for (String mainId : mainIdsMap.get(primaryId)) {
                        if (!first) {
                            sb.append(",");
                        } else {
                            first = false;
                        }
                        sb.append(mainId);
                    }
                }

                if (synonymMap != null && synonymMap.containsKey(primaryId)) {
                    boolean first = true;
                    sb.append("\t");
                    for (String synonym : synonymMap.get(primaryId)) {
                        if (!first) {
                            sb.append(",");
                        } else {
                            first = false;
                        }
                        sb.append(synonym);
                    }
                }
                sb.append(System.getProperty("line.separator"));
                fw.write(sb.toString());
            }
        }
        fw.flush();
        fw.close();
    }


    /**
     * Read contents of an IdResolver from file, allows for caching during a build.
     * @param f the file to read from
     * @throws IOException if problem reading from file
     */
    public void populateFromFile(File f) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] cols = line.split("\t");
            String taxonId = cols[0];
            String clsName = cols[1];
            String primaryId = cols[2];

            String mainIdsStr = cols[3];
            if (!StringUtils.isBlank(mainIdsStr)) {
                String[] mainIds = mainIdsStr.split(",");
                addEntry(taxonId, clsName, primaryId, Arrays.asList(mainIds), Boolean.TRUE);
            }

            // read synonyms if they are present
            if (cols.length >= 5) {
                String synonymsStr = cols[4];
                if (!StringUtils.isBlank(synonymsStr)) {
                    String[] synonyms = synonymsStr.split(",");
                    addEntry(taxonId, clsName, primaryId, Arrays.asList(synonyms), Boolean.FALSE);
                }
            }
        }
        reader.close();
    }

    // add a new list to a map or add elements of set to existing map entry
    private void addToMapList(Map<String, Set<String>> map, String key, Collection<String> values) {
        Set<String> set = map.get(key);
        if (set == null) {
            set = new LinkedHashSet<String>();
            map.put(key, set);
        }
        set.addAll(values);
    }
}
