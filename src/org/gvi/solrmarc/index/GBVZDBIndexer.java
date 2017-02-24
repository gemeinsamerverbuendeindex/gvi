/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gvi.solrmarc.index;

import org.marc4j.marc.Record;

/**
 *
 * @author Thomas Kirchhoff <thomas.kirchhoff@bsz-bw.de>
 */
public class GBVZDBIndexer extends GVIIndexer
{
    
    public GBVZDBIndexer(String indexingPropsFile, String[] propertyDirs)
    {
        super(indexingPropsFile, propertyDirs);
    }

    @Override
    protected boolean isGbvZdbRecord(Record record)
    {
        return true;
    }
    
    
}
