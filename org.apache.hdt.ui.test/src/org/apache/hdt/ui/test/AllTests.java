/**
 * 
 */
package org.apache.hdt.ui.test;

import org.apache.hdt.ui.test.hdfs.HDFSTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.Test;
import junit.framework.TestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	HDFSTests.class
})
/**
 * @author Srimanth Gunturi
 *
 */
public class AllTests {}
