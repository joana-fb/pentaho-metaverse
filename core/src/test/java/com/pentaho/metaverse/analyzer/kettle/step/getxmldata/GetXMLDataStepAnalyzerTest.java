/*
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2015 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

package com.pentaho.metaverse.analyzer.kettle.step.getxmldata;

import com.pentaho.dictionary.DictionaryConst;
import com.pentaho.metaverse.api.IAnalysisContext;
import com.pentaho.metaverse.api.IComponentDescriptor;
import com.pentaho.metaverse.api.IMetaverseBuilder;
import com.pentaho.metaverse.api.IMetaverseNode;
import com.pentaho.metaverse.api.INamespace;
import com.pentaho.metaverse.api.MetaverseComponentDescriptor;
import com.pentaho.metaverse.api.StepField;
import com.pentaho.metaverse.api.analyzer.kettle.ComponentDerivationRecord;
import com.pentaho.metaverse.api.analyzer.kettle.step.ExternalResourceStepAnalyzer;
import com.pentaho.metaverse.api.analyzer.kettle.step.StepNodes;
import com.pentaho.metaverse.api.model.IExternalResourceInfo;
import com.pentaho.metaverse.testutils.MetaverseTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.getxmldata.GetXMLData;
import org.pentaho.di.trans.steps.getxmldata.GetXMLDataField;
import org.pentaho.di.trans.steps.getxmldata.GetXMLDataMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by mburgess on 4/24/15.
 */
@RunWith( MockitoJUnitRunner.class )
public class GetXMLDataStepAnalyzerTest {

  GetXMLDataStepAnalyzer analyzer;

  @Mock GetXMLDataMeta meta;
  @Mock INamespace mockNamespace;
  @Mock IMetaverseNode node;
  @Mock IMetaverseBuilder builder;
  @Mock TransMeta parentTransMeta;
  @Mock StepMeta parentStepMeta;
  @Mock RowMetaInterface rmi;
  @Mock GetXMLData data;

  IComponentDescriptor descriptor;

  @Before
  public void setUp() throws Exception {
    when( mockNamespace.getParentNamespace() ).thenReturn( mockNamespace );
    descriptor = new MetaverseComponentDescriptor( "test", DictionaryConst.NODE_TYPE_TRANS_STEP, mockNamespace );
    analyzer = spy( new GetXMLDataStepAnalyzer() );
    analyzer.setDescriptor( descriptor );
    analyzer.setObjectFactory( MetaverseTestUtils.getMetaverseObjectFactory() );
    analyzer.setRootNode( node );
    analyzer.setParentTransMeta( parentTransMeta );
    analyzer.setParentStepMeta( parentStepMeta );
  }

  @Test
  public void testGetResourceInputNodeType() throws Exception {
    assertEquals( DictionaryConst.NODE_TYPE_FILE_FIELD, analyzer.getResourceInputNodeType() );
  }

  @Test
  public void testGetResourceOutputNodeType() throws Exception {
    assertNull( analyzer.getResourceOutputNodeType() );
  }

  @Test
  public void testIsOutput() throws Exception {
    assertFalse( analyzer.isOutput() );
  }

  @Test
  public void testIsInput() throws Exception {
    assertTrue( analyzer.isInput() );
  }

  @Test
  public void testGetSupportedSteps() {
    GetXMLDataStepAnalyzer analyzer = new GetXMLDataStepAnalyzer();
    Set<Class<? extends BaseStepMeta>> types = analyzer.getSupportedSteps();
    assertNotNull( types );
    assertEquals( types.size(), 1 );
    assertTrue( types.contains( GetXMLDataMeta.class ) );
  }

  @Test
  public void testGetUsedFields_xmlNotInField() throws Exception {
    when( meta.isInFields() ).thenReturn( false );
    Set<StepField> usedFields = analyzer.getUsedFields( meta );
    assertEquals( 0, usedFields.size() );
  }

  @Test
  public void testGetUsedFields() throws Exception {
    when( meta.isInFields() ).thenReturn( true );
    when( meta.getXMLField() ).thenReturn( "xml" );

    StepNodes inputs = new StepNodes();
    inputs.addNode( "previousStep", "xml", node );
    inputs.addNode( "previousStep", "otherField", node );
    doReturn( inputs ).when( analyzer ).getInputs();

    Set<StepField> usedFields = analyzer.getUsedFields( meta );
    assertEquals( 1, usedFields.size() );
    assertEquals( "xml", usedFields.iterator().next().getFieldName() );
  }

  @Test
  public void testCreateResourceNode() throws Exception {
    IExternalResourceInfo res = mock( IExternalResourceInfo.class );
    when( res.getName() ).thenReturn( "file:///Users/home/tmp/xyz.xml" );
    IMetaverseNode resourceNode = analyzer.createResourceNode( res );
    assertNotNull( resourceNode );
    assertEquals( DictionaryConst.NODE_TYPE_FILE, resourceNode.getType() );
  }

  @Test
  public void testCreateOutputFieldNode() throws Exception {
    doReturn( builder ).when( analyzer ).getMetaverseBuilder();
    analyzer.setBaseStepMeta( meta );

    GetXMLDataField[] fields = new GetXMLDataField[2];
    GetXMLDataField field1 = new GetXMLDataField( "name" );
    GetXMLDataField field2 = new GetXMLDataField( "age" );
    field1.setXPath( "field1/xpath" );
    field2.setElementType( 1 );
    field1.setResultType( 1 );
    field2.setRepeated( true );

    fields[0] = field1;
    fields[1] = field2;
    when( meta.getInputFields() ).thenReturn( fields );

    IAnalysisContext context = mock( IAnalysisContext.class );
    doReturn( "thisStepName" ).when( analyzer ).getStepName();
    when( node.getLogicalId() ).thenReturn( "logical id" );
    ValueMetaInterface vmi = new ValueMeta( "name", 1 );

    IMetaverseNode outputFieldNode = analyzer.createOutputFieldNode(
      context,
      vmi,
      ExternalResourceStepAnalyzer.RESOURCE,
      DictionaryConst.NODE_TYPE_TRANS_FIELD );

    assertNotNull( outputFieldNode );

    assertNotNull( outputFieldNode.getProperty( DictionaryConst.PROPERTY_KETTLE_TYPE ) );
    assertEquals( ExternalResourceStepAnalyzer.RESOURCE,
      outputFieldNode.getProperty( DictionaryConst.PROPERTY_TARGET_STEP ) );
    assertEquals( "field1/xpath", outputFieldNode.getProperty( "xpath" ) );
    assertNotNull( outputFieldNode.getProperty( "resultType" ) );
    assertNotNull( outputFieldNode.getProperty( "element" ) );
    assertEquals( false, outputFieldNode.getProperty( "repeat" ) );

    // the input node should be added by this step
    verify( builder ).addNode( outputFieldNode );

  }

  @Test
  public void testGetInputRowMetaInterfaces_isInFields() throws Exception {
    when( parentTransMeta.getPrevStepNames( parentStepMeta ) ).thenReturn( null );

    RowMetaInterface rowMetaInterface = mock( RowMetaInterface.class );
    doReturn( rowMetaInterface ).when( analyzer ).getOutputFields( meta );
    when( meta.isInFields() ).thenReturn( true );
    when( meta.getIsAFile() ).thenReturn( false );
    when( meta.isReadUrl() ).thenReturn( false );

    Map<String, RowMetaInterface> rowMetaInterfaces = analyzer.getInputRowMetaInterfaces( meta );
    assertNotNull( rowMetaInterfaces );
    assertEquals( 0, rowMetaInterfaces.size() );
  }

  @Test
  public void testGetInputRowMetaInterfaces_isNotInField() throws Exception {
    Map<String, RowMetaInterface> inputs = new HashMap<>();
    RowMetaInterface inputRmi = mock( RowMetaInterface.class );

    List<ValueMetaInterface> vmis = new ArrayList<>();
    ValueMetaInterface vmi = new ValueMeta( "filename" );
    vmis.add( vmi );

    when( inputRmi.getValueMetaList() ).thenReturn( vmis );
    inputs.put( "test", inputRmi );
    doReturn( inputs ).when( analyzer ).getInputFields( meta );
    when( parentTransMeta.getPrevStepNames( parentStepMeta ) ).thenReturn( null );

    RowMetaInterface rowMetaInterface = new RowMeta();
    rowMetaInterface.addValueMeta( vmi );
    ValueMetaInterface vmi2 = new ValueMeta( "otherField" );
    rowMetaInterface.addValueMeta( vmi2 );

    doReturn( rowMetaInterface ).when( analyzer ).getOutputFields( meta );
    when( meta.isInFields() ).thenReturn( false );
    when( meta.getIsAFile() ).thenReturn( false );
    when( meta.isReadUrl() ).thenReturn( false );

    Map<String, RowMetaInterface> rowMetaInterfaces = analyzer.getInputRowMetaInterfaces( meta );
    assertNotNull( rowMetaInterfaces );
    assertEquals( 2, rowMetaInterfaces.size() );
    RowMetaInterface metaInterface = rowMetaInterfaces.get( ExternalResourceStepAnalyzer.RESOURCE );
    // the row meta interface should only have 1 value meta in it, and it should NOT be filename
    assertEquals( 1, metaInterface.size() );
    assertEquals( "otherField", metaInterface.getFieldNames()[0] );
  }

  @Test
  public void testGetChangeRecords() throws Exception {

    when( meta.isInFields() ).thenReturn( true );
    when( meta.getIsAFile() ).thenReturn( false );
    when( meta.isReadUrl() ).thenReturn( false );
    when( meta.getXMLField() ).thenReturn( "xml" );
    analyzer.setBaseStepMeta( meta );

    GetXMLDataField[] fields = new GetXMLDataField[2];
    GetXMLDataField field1 = new GetXMLDataField( "name" );
    GetXMLDataField field2 = new GetXMLDataField( "age" );
    field1.setXPath( "field1/xpath" );
    field2.setElementType( 1 );
    field1.setResultType( 1 );
    field2.setRepeated( true );

    fields[0] = field1;
    fields[1] = field2;
    when( meta.getInputFields() ).thenReturn( fields );

    StepNodes inputs = new StepNodes();
    inputs.addNode( "previousStep", "xml", node );
    doReturn( inputs ).when( analyzer ).getInputs();

    Set<ComponentDerivationRecord> changeRecords = analyzer.getChangeRecords( meta );
    assertNotNull( changeRecords );
    assertEquals( 2, changeRecords.size() );
  }

  @Test
  public void testCustomAnalyze() throws Exception {
    when( meta.getLoopXPath() ).thenReturn( "file/xpath/name" );
    analyzer.customAnalyze( meta, node );
    verify( node ).setProperty( "loopXPath", "file/xpath/name" );
  }


  @Test
  public void testGetXMLDataExternalResourceConsumer() throws Exception {
    GetXMLDataExternalResourceConsumer consumer = new GetXMLDataExternalResourceConsumer();

    StepMeta spyMeta = spy( new StepMeta( "test", meta ) );

    when( meta.getParentStepMeta() ).thenReturn( spyMeta );
    when( spyMeta.getParentTransMeta() ).thenReturn( parentTransMeta );
    when( data.getStepMetaInterface() ).thenReturn( meta );

    when( meta.isInFields() ).thenReturn( false );
    String[] filePaths = { "/path/to/file1", "/another/path/to/file2" };
    when( meta.getFileName() ).thenReturn( filePaths );
    when( parentTransMeta.environmentSubstitute( any( String[].class ) ) ).thenReturn( filePaths );

    assertFalse( consumer.isDataDriven( meta ) );
    Collection<IExternalResourceInfo> resources = consumer.getResourcesFromMeta( meta );
    assertFalse( resources.isEmpty() );
    assertEquals( 2, resources.size() );


    when( meta.isInFields() ).thenReturn( true );
    when( meta.getIsAFile() ).thenReturn( true );
    assertTrue( consumer.isDataDriven( meta ) );
    assertTrue( consumer.getResourcesFromMeta( meta ).isEmpty() );
    when( rmi.getString( Mockito.any( Object[].class ), anyString(), anyString() ) )
      .thenReturn( "/path/to/row/file" );
    resources = consumer.getResourcesFromRow( data, rmi, new String[]{ "id", "name" } );
    assertFalse( resources.isEmpty() );
    assertEquals( 1, resources.size() );

    when( rmi.getString( Mockito.any( Object[].class ), anyString(), anyString() ) )
      .thenThrow( KettleException.class );
    resources = consumer.getResourcesFromRow( data, rmi, new String[]{ "id", "name" } );
    assertTrue( resources.isEmpty() );

    assertEquals( GetXMLDataMeta.class, consumer.getMetaClass() );
  }

}
