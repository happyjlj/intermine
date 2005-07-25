package org.flymine.postprocess;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;

import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.Map.Entry;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Utility methods for finding overlaps.
 *
 * @author Kim Rutherford
 */

public abstract class OverlapUtil
{
    /**
     * Return an Iterator for pairs of overlapping LocatedSequenceFeature objects that are located
     * on the given subject (generally a Chromosome).
     * @param os the ObjectStore to query
     * @param subject the LocatedSequenceFeature (eg. a Chromosome) where the LSFs are located
     * @return an Iterator over the overlapping features
     * @throws ObjectStoreException if an error occurs while writing
     */
    public static Iterator findOverlaps(final ObjectStore os, LocatedSequenceFeature subject)
        throws ObjectStoreException {
        Query q = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);

        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);

        q.setDistinct(false);
        QueryClass qcObj = new QueryClass(LocatedSequenceFeature.class);
        q.addFrom(qcObj);
        q.addToSelect(qcObj);

        QueryClass qcSub = new QueryClass(BioEntity.class);
        q.addFrom(qcSub);

        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "subject");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
        cs.addConstraint(cc1);

        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "object");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
        cs.addConstraint(cc2);

        QueryField subIdQF = new QueryField(qcSub, "id");
        SimpleConstraint subjectIdConstraint =
            new SimpleConstraint(subIdQF, ConstraintOp.EQUALS, new QueryValue(subject.getId()));

        cs.addConstraint(subjectIdConstraint);

        if (subject instanceof Chromosome) {
            // improve the speed by adding an extra contain for locations on chromosomes
            QueryObjectReference objChromosomeRef = new QueryObjectReference(qcObj, "chromosome");
            ContainsConstraint chromosomeConstraint =
                new ContainsConstraint(objChromosomeRef, ConstraintOp.CONTAINS, qcSub);

            cs.addConstraint(chromosomeConstraint);
        }

        ((ObjectStoreInterMineImpl) os).precompute(q);

        Results results = os.execute(q);
        results.setBatchSize(5000);

        // ordered by length - largest first
        final TreeMap locationsByLength = new TreeMap(new SimpleLocLengthComparator());

        // ordered by start position - smallest first
        final TreeMap locationsByStartPos = new TreeMap(new SimpleLocStartComparator());

        Iterator resIter = results.iterator();

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();

            Location location = (Location) rr.get(0);
            LocatedSequenceFeature lsf = (LocatedSequenceFeature) rr.get(1);
            SimpleLoc simpleLoc = new SimpleLoc(location);

            if (lsf.getLength() == null) {
                throw new RuntimeException("found a LocatedSequenceFeature with a null length");
            }

            locationsByLength.put(simpleLoc, simpleLoc);
            locationsByStartPos.put(simpleLoc, simpleLoc);
        }

        return new OverlappingFeaturesIterator(os, locationsByLength, locationsByStartPos);
    }

    /**
     * The Iterator returned by findOverlaps().
     */
    private static final class OverlappingFeaturesIterator implements Iterator {
        private final ObjectStore os;
        private final TreeMap locationsByStartPos;
        private Iterator locationsByLengthIter = null;
        private Iterator otherLocationIter = null;
        private Location currentLocation = null;

        private OverlappingFeaturesIterator(ObjectStore os, TreeMap locationsByLengthMap, 
                                            TreeMap locationsByStartPos) {
            super();
            this.os = os;
            this.locationsByStartPos = locationsByStartPos;
            locationsByLengthIter = locationsByLengthMap.entrySet().iterator();
        }

        public boolean hasNext() {
            if (otherLocationIter != null && otherLocationIter.hasNext()) {
                return true;
            } else {
                while (locationsByLengthIter.hasNext()) {
                    Map.Entry entry = (Entry) locationsByLengthIter.next();
        
                    SimpleLoc simpleLoc = (SimpleLoc) entry.getValue();
                    Integer length = new Integer(simpleLoc.length);
        
                    try {
                        currentLocation =
                            (Location) os.getObjectById(new Integer(simpleLoc.id));
                    } catch (ObjectStoreException e) {
                        throw new RuntimeException("A Location has gone missing during "
                                                   + "processing", e);
                    }
        
                    List overlappingLocations =
                        getOverlappingLocations(simpleLoc, length.intValue());
        
                    // remove from the locationsByStartPos Map so that we find each overlapping
                    // pair once only
                    removeFromLocationsFromStartPos(simpleLoc);
        
                    otherLocationIter = overlappingLocations.iterator();
        
                    if (otherLocationIter.hasNext()) {
                        return true;
                    }
                }
        
                return false;
            }
        }

        public Object next() throws NoSuchElementException {
                Location returnArray[] = new Location[2];
                returnArray[0] = currentLocation;
                returnArray[1] = (Location) otherLocationIter.next();
                return returnArray;
           }

        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        private void removeFromLocationsFromStartPos(SimpleLoc simpleLoc) {
            Map subMap = locationsByStartPos.subMap(new Integer(simpleLoc.start),
                                                    new Integer(simpleLoc.end + 1));
            Iterator subMapIter = subMap.entrySet().iterator();
        
            while (subMapIter.hasNext()) {
                Map.Entry entry = (Map.Entry) subMapIter.next();
                SimpleLoc thisSimpleLoc = (SimpleLoc) entry.getValue();
        
                if (simpleLoc == thisSimpleLoc) {
                    subMapIter.remove();
                }
            }
        }

        // look at a range between start-length .. end
        // we know that we don't need to look further back because we are examining the
        // locations in reverse length order
        private List getOverlappingLocations(SimpleLoc thisSimpleLoc, int length) {
            List overlappingLocations = new ArrayList();
        
            Map subRange =
                locationsByStartPos.subMap(new Integer(thisSimpleLoc.start - length),
                                           new Integer(thisSimpleLoc.end + 1));
        
            Iterator subRangeIter = subRange.values().iterator();
        
            while (subRangeIter.hasNext()) {
                SimpleLoc otherSimpleLoc = (SimpleLoc) subRangeIter.next();
        
                if (thisSimpleLoc != otherSimpleLoc &&
                    (thisSimpleLoc.start <= otherSimpleLoc.start &&
                     thisSimpleLoc.end >= otherSimpleLoc.start ||
                     thisSimpleLoc.start <= otherSimpleLoc.end &&
                     thisSimpleLoc.end >= otherSimpleLoc.end)) {
                    Location originalLocation;
                    try {
                        originalLocation =
                            (Location) os.getObjectById(new Integer(otherSimpleLoc.id));
                    } catch (ObjectStoreException e) {
                        throw new RuntimeException("A Location has gone missing during "
                                                   + "processing", e);
                    }
                    overlappingLocations.add(originalLocation);
                }
            }
        
            return overlappingLocations;
        }
    }

    /**
     * A Comparator that compares SimpleLoc objects by start position, or by id for pairs of
     * SimpleLoc objects that have the same start position.
     */
    static private class SimpleLocStartComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            int loc1Start = -1;
            int loc2Start = -1;

            SimpleLoc loc1 = null;
            SimpleLoc loc2 = null;

            if (o1 instanceof SimpleLoc) {
                loc1 = (SimpleLoc) o1;
                loc1Start = loc1.start;
            } else {
                // get() is being called with an Integer
                loc1Start = ((Integer) o1).intValue();
            }

            if (o2 instanceof SimpleLoc) {
                loc2 = (SimpleLoc) o2;
                loc2Start = loc2.start;
            } else {
                // get() is being called with an Integer
                loc2Start = ((Integer) o2).intValue();
            }

            if (loc1Start < loc2Start) {
                return -1;
            } else {
                if (loc1Start > loc2Start) {
                    return 1;
                } else {
                    if (loc1 == null) {
                        return -1;
                    }
                    if (loc2 == null) {
                        return 1;
                    }

                    if (loc1.id < loc2.id) {
                        return -1;
                    } else {
                        if (loc1.id > loc2.id) {
                            return 1;
                        } else {
                            return 0;
                            }
                    }
                }
            }
        }
    }

    /**
     * A Comparator that compares SimpleLoc objects by length, or by id for pairs of
     * SimpleLoc objects that have the same length position.  Longer locations are ordered first.
     */
    static private class SimpleLocLengthComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 instanceof SimpleLoc && o2 instanceof SimpleLoc) {
                SimpleLoc loc1 = (SimpleLoc) o1;
                SimpleLoc loc2 = (SimpleLoc) o2;
                if (loc1.length > loc2.length) {
                    return -1;
                } else {
                    if (loc1.length < loc2.length) {
                        return 1;
                    } else {
                        if (loc1.id < loc2.id) {
                            return -1;
                        } else {
                            if (loc1.id > loc2.id) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    }
                }

            } else {
                throw new RuntimeException("argument to compare() is not a SimpleLoc");
            }
        }
    }

    /**
     * A simplified Location object that is much smaller than a DynamicBean.
     */
    static private class SimpleLoc {
        int start;
        int end;
        int length;
        int id;

        public SimpleLoc(Location location) {
            start = location.getStart().intValue();
            end = location.getEnd().intValue();
            length = end - start + 1;
            id = location.getId().intValue();
        }
    }
}

