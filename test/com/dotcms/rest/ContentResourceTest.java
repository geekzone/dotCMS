package com.dotcms.rest;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.DataType;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcher;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.ibm.icu.util.Calendar;
import com.liferay.portal.model.User;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

import edu.emory.mathcs.backport.java.util.Arrays;

public class ContentResourceTest extends TestBase {
    Client client;
    WebResource contRes;
    String authheader="Authorization";
    String authvalue="Basic "+new String(Base64.encode("admin@dotcms.com:admin"));
    
    @Before
    public void before() {
        client=Client.create();
        HttpServletRequest request = ServletTestRunner.localRequest.get();
        String serverName = request.getServerName();
        long serverPort = request.getServerPort();
        contRes = client.resource("http://"+serverName+":"+serverPort+"/api/content");
    }
    
    @Test
    public void singlePUT() throws Exception {
        Structure st=StructureCache.getStructureByVelocityVarName("webPageContent");
        Host demo=APILocator.getHostAPI().findByName("demo.dotcms.com", APILocator.getUserAPI().getSystemUser(), false);
        User sysuser=APILocator.getUserAPI().getSystemUser();
        String demoId=demo.getIdentifier();
        ClientResponse response=
                contRes.path("/publish/1").type(MediaType.APPLICATION_JSON_TYPE)
                       .header(authheader, authvalue).put(ClientResponse.class,
                                new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "Test content from ContentResourceTest")
                                .put("body", "this is an example text")
                                .put("contentHost", demoId).toString());
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().contains("/api/content/inode/"));
        String location=response.getLocation().toString();
        String inode=location.substring(location.lastIndexOf("/")+1);
        Contentlet cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        Assert.assertEquals(demoId, cont.getHost());
        Assert.assertEquals(st.getInode(), cont.getStructureInode());
        Assert.assertEquals(1,cont.getLanguageId());
        Assert.assertEquals("Test content from ContentResourceTest",cont.getStringProperty("title"));
        Assert.assertEquals("this is an example text",cont.getStringProperty("body"));
        Assert.assertTrue(cont.isLive());
        
        // testing other host_or_folder formats: folderId
        Folder folder=APILocator.getFolderAPI().findFolderByPath("/home", demo, sysuser, false);
        response=contRes.path("/publish/1").type(MediaType.APPLICATION_JSON_TYPE)
                       .header(authheader, authvalue).put(ClientResponse.class,
                                new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "Test content from ContentResourceTest (folderId)")
                                .put("body", "this is an example text")
                                .put("contentHost", folder.getInode()).toString());
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertEquals(folder.getInode(), cont.getFolder());
        Assert.assertTrue(cont.isLive());
        
        // testing other host_or_folder formats: hostname
        response=contRes.path("/publish/1").type(MediaType.APPLICATION_JSON_TYPE)
                .header(authheader, authvalue).put(ClientResponse.class,
                        new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "Test content from ContentResourceTest (folderId)")
                                .put("body", "this is an example text")
                                .put("contentHost", "demo.dotcms.com").toString());
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertEquals(demoId, cont.getHost());
        Assert.assertTrue(cont.isLive());
        
        // testing other host_or_folder formats: hostname:path
        response=contRes.path("/justsave/1").type(MediaType.APPLICATION_JSON_TYPE)
                .header(authheader, authvalue).put(ClientResponse.class,
                        new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "Test content from ContentResourceTest (folderId)")
                                .put("body", "this is an example text")
                                .put("contentHost", "demo.dotcms.com:/home").toString());
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertEquals(folder.getInode(), cont.getFolder());
        Assert.assertFalse(cont.isLive());
        
        
        // testing XML
        response=contRes.path("/publish/1").type(MediaType.APPLICATION_XML_TYPE)
                       .header(authheader, authvalue).put(ClientResponse.class,
                            "<content>" +
                            "<stInode>" +st.getInode() + "</stInode>"+
                            "<languageId>1</languageId>"+
                            "<title>Test content from ContentResourceTest XML</title>"+
                            "<body>this is an example text XML</body>"+
                            "<contentHost>"+demoId+"</contentHost>"+
                            "</content>");
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().contains("/api/content/inode/"));
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        Assert.assertEquals(demoId, cont.getHost());
        Assert.assertEquals(st.getInode(), cont.getStructureInode());
        Assert.assertEquals(1,cont.getLanguageId());
        Assert.assertEquals("Test content from ContentResourceTest XML",cont.getStringProperty("title"));
        Assert.assertEquals("this is an example text XML",cont.getStringProperty("body"));
        Assert.assertTrue(cont.isLive());
        
        // testing form-urlencoded
        String title="Test content from ContentResourceTest FORM "+UUIDGenerator.generateUuid();
        String body="this is an example text FORM "+UUIDGenerator.generateUuid();
        response=contRes.path("/publish/1").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .header(authheader, authvalue).put(ClientResponse.class,
                     "stInode=" +st.getInode() + "&"+
                     "languageId=1&"+
                     "title="+URLEncoder.encode(title, "UTF-8")+"&"+
                     "body="+URLEncoder.encode(body, "UTF-8")+"&"+
                     "contentHost="+demoId);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().contains("/api/content/inode/"));
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        Assert.assertEquals(inode, response.getHeaders().getFirst("inode")); // validate consistency of inode header
        cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        Assert.assertEquals(cont.getIdentifier(), response.getHeaders().getFirst("identifier")); // consistency of identifier header
        Assert.assertEquals(demoId, cont.getHost());
        Assert.assertEquals(st.getInode(), cont.getStructureInode());
        Assert.assertEquals(1,cont.getLanguageId());
        Assert.assertEquals(title,cont.getStringProperty("title"));
        Assert.assertEquals(body,cont.getStringProperty("body"));
        Assert.assertTrue(cont.isLive());

        
    }
    
    @Test
    public void multipartPUT() throws Exception {
        final String salt=Long.toString(System.currentTimeMillis());
        final User sysuser=APILocator.getUserAPI().getSystemUser();
        
        ClientResponse response = contRes.path("/publish/1").type(MediaType.MULTIPART_FORM_DATA_TYPE)
                                   .header(authheader, authvalue).put(ClientResponse.class, 
                                           new MultiPart()
                                             .bodyPart(new BodyPart(
                                                     new JSONObject()
                                                        .put("hostFolder", "demo.dotcms.com:/resources")
                                                        .put("title", "newfile"+salt+".txt")
                                                        .put("fileName", "newfile"+salt+".txt")
                                                        .put("languageId", "1")
                                                        .put("stInode", StructureCache.getStructureByVelocityVarName("FileAsset").getInode())
                                                        .toString(), MediaType.APPLICATION_JSON_TYPE))
                                             .bodyPart(new StreamDataBodyPart(
                                                         "newfile"+salt+".txt", 
                                                         new ByteArrayInputStream(("this is the salt "+salt).getBytes()),
                                                         "newfile"+salt+".txt",
                                                         MediaType.APPLICATION_OCTET_STREAM_TYPE)));
        Assert.assertEquals(200, response.getStatus());
        Contentlet cont=APILocator.getContentletAPI().find(response.getHeaders().getFirst("inode"),sysuser,false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        Assert.assertTrue(response.getLocation().toString().endsWith("/api/content/inode/"+cont.getInode()));
        FileAsset file=APILocator.getFileAssetAPI().fromContentlet(cont);
        Assert.assertEquals("/resources/newfile"+salt+".txt",file.getURI());
        Assert.assertEquals("demo.dotcms.com", APILocator.getHostAPI().find(file.getHost(), sysuser, false).getHostname());
        Assert.assertEquals("this is the salt "+salt, IOUtils.toString(file.getFileInputStream()));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void categoryAndTagFields() throws Exception {
        User sysuser=APILocator.getUserAPI().getSystemUser();
        Structure st=StructureCache.getStructureByVelocityVarName("Blog");
        String salt=Long.toString(System.currentTimeMillis());
        ClientResponse response=contRes.path("/justsave/1").type(MediaType.APPLICATION_JSON_TYPE)
                .header(authheader, authvalue).put(ClientResponse.class,
                        new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("host1", "demo.dotcms.com")
                                .put("title", "blog post "+salt)
                                .put("urlTitle", "blog-post-"+salt)
                                .put("author", "junit")
                                .put("sysPublishDate", new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")
                                                          .format(Calendar.getInstance().getTime()))
                                .put("body","blog post content "+salt)
                                .put("topic", "investing,banking")
                                .put("tags", "junit,integration tests,jenkins")
                                .put("contentHost", "demo.dotcms.com:/home").toString());
        Assert.assertEquals(200, response.getStatus());
        String inode=response.getHeaders().getFirst("inode");
        Contentlet cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        
        /////////////////////////
        // checking categories //
        /////////////////////////
        
        List<Category> cats=APILocator.getCategoryAPI().getParents(cont, sysuser, false);
        Assert.assertNotNull(cats);
        Assert.assertEquals(2,cats.size());
        
        Set<String> expectedIds=new HashSet<String>();
        expectedIds.add("investing"); expectedIds.add("banking");
        expectedIds.remove(cats.get(0).getCategoryVelocityVarName());
        expectedIds.remove(cats.get(1).getCategoryVelocityVarName());
        Assert.assertEquals(0, expectedIds.size());
        
        ///////////////////
        // checking tags //
        ///////////////////
        
        List<Tag> tags=APILocator.getTagAPI().getTagsByInode(cont.getInode());
        Assert.assertNotNull(tags);
        Assert.assertEquals(3, tags.size());
        Set<String> expectedTags=new HashSet<String>(Arrays.asList("junit,integration tests,jenkins".split(",")));
        for(Tag tt : tags) {
            Assert.assertTrue(expectedTags.remove(tt.getTagName()));
        }
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void workflowTask() throws  Exception {
        final String salt=Long.toString(System.currentTimeMillis());
                
        // a mandatory scheme to test
        WorkflowScheme scheme = new WorkflowScheme();
        scheme.setMandatory(true);
        scheme.setName("Rest Mandatory Workflow "+salt);
        scheme.setDescription("testing rest save content");
        scheme.setCreationDate(new Date());
        APILocator.getWorkflowAPI().saveScheme(scheme);
        
        WorkflowStep step1=new WorkflowStep();
        step1.setCreationDate(new Date());
        step1.setEnableEscalation(false);
        step1.setMyOrder(1);
        step1.setName("Step 1");
        step1.setResolved(false);
        step1.setSchemeId(scheme.getId());
        APILocator.getWorkflowAPI().saveStep(step1);
        
        WorkflowStep step2=new WorkflowStep();
        step2.setCreationDate(new Date());
        step2.setEnableEscalation(false);
        step2.setMyOrder(2);
        step2.setName("Step 2");
        step2.setResolved(false);
        step2.setSchemeId(scheme.getId());
        APILocator.getWorkflowAPI().saveStep(step2);
        
        WorkflowStep step3=new WorkflowStep();
        step3.setCreationDate(new Date());
        step3.setEnableEscalation(false);
        step3.setMyOrder(3);
        step3.setName("Step 3");
        step3.setResolved(true);
        step3.setSchemeId(scheme.getId());
        APILocator.getWorkflowAPI().saveStep(step3);
        
        // Save as Draft Step1 -> Step1
        WorkflowAction saveDraft=new WorkflowAction();
        saveDraft.setId(UUIDGenerator.generateUuid());
        saveDraft.setName("Save as Draft");
        saveDraft.setOrder(1);
        saveDraft.setNextStep(step1.getId());
        saveDraft.setRequiresCheckout(true);
        saveDraft.setStepId(step1.getId());
        saveDraft.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        APILocator.getWorkflowAPI().saveAction(saveDraft, 
                Arrays.asList(new Permission[] { 
                        new Permission(
                                saveDraft.getPermissionType(),
                                saveDraft.getId(),
                                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                                PermissionAPI.PERMISSION_USE) }));
        
     // Save as Draft Step1 -> Step1
        WorkflowAction escalate=new WorkflowAction();
        escalate.setId(UUIDGenerator.generateUuid());
        escalate.setName("Save and Assign");
        escalate.setOrder(2);
        escalate.setNextStep(step1.getId());
        escalate.setRequiresCheckout(true);
        escalate.setStepId(step1.getId());
        escalate.setAssignable(true);
        escalate.setCommentable(true);
        escalate.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        APILocator.getWorkflowAPI().saveAction(escalate, 
                Arrays.asList(new Permission[] { 
                        new Permission(
                                escalate.getPermissionType(),
                                escalate.getId(),
                                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                                PermissionAPI.PERMISSION_USE) }));
        
        // Send for review Step1 -> Step2
        WorkflowAction sendReview=new WorkflowAction();
        sendReview.setId(UUIDGenerator.generateUuid());
        sendReview.setName("Send for review");
        sendReview.setOrder(3);
        sendReview.setNextStep(step2.getId());
        sendReview.setRequiresCheckout(false);
        sendReview.setStepId(step1.getId());
        sendReview.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        APILocator.getWorkflowAPI().saveAction(sendReview, 
                Arrays.asList(new Permission[] { 
                        new Permission(
                                sendReview.getPermissionType(),
                                sendReview.getId(),
                                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                                PermissionAPI.PERMISSION_USE) }));
        
        // reject Step2 -> Step1
        WorkflowAction reject=new WorkflowAction();
        reject.setId(UUIDGenerator.generateUuid());
        reject.setName("Reject");
        reject.setOrder(1);
        reject.setNextStep(step1.getId());
        reject.setRequiresCheckout(false);
        reject.setStepId(step2.getId());
        reject.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        APILocator.getWorkflowAPI().saveAction(reject, 
                Arrays.asList(new Permission[] { 
                        new Permission(
                                reject.getPermissionType(),
                                reject.getId(),
                                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                                PermissionAPI.PERMISSION_USE) }));
        
        // publish Step2 -> Step3
        WorkflowAction publish=new WorkflowAction();
        publish.setId(UUIDGenerator.generateUuid());
        publish.setName("Publish");
        publish.setOrder(2);
        publish.setNextStep(step3.getId());
        publish.setRequiresCheckout(false);
        publish.setStepId(step2.getId());
        publish.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        APILocator.getWorkflowAPI().saveAction(publish, 
                Arrays.asList(new Permission[] { 
                        new Permission(
                                publish.getPermissionType(),
                                publish.getId(),
                                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                                PermissionAPI.PERMISSION_USE) }));
        WorkflowActionClass publishlet=new WorkflowActionClass();
        publishlet.setActionId(publish.getId());
        publishlet.setClazz(com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet.class.getCanonicalName());
        publishlet.setName("publish");
        publishlet.setOrder(1);
        APILocator.getWorkflowAPI().saveActionClass(publishlet);
        
        // a test structure with that scheme
        Structure st=new Structure();
        st.setName("Rest test st "+salt);
        st.setVelocityVarName("restTestSt"+salt);
        st.setDescription("testing rest content creation with mandatory workflow");
        StructureFactory.saveStructure(st);
        Field field=new Field("Title",FieldType.TEXT,DataType.TEXT,st,true,true,true,1,false,false,true);
        FieldFactory.saveField(field);
        APILocator.getWorkflowAPI().saveSchemeForStruct(st, scheme);
        
        // send the Rest api call
        User sysuser=APILocator.getUserAPI().getSystemUser();
        User bill=APILocator.getUserAPI().loadUserById("dotcms.org.2806");
        Role billrole=APILocator.getRoleAPI().getUserRole(bill);
        ClientResponse response=contRes.path("/Save%20and%20Assign/1/wfActionComments/please%20do%20this%20for%20me/wfActionAssign/"+billrole.getId())
            .type(MediaType.APPLICATION_JSON_TYPE)
            .header(authheader, authvalue).put(ClientResponse.class,
                new JSONObject()
                    .put("stInode", st.getInode())
                    .put("languageId", 1)
                    .put(field.getVelocityVarName(), "test title "+salt)
                    .toString());
        Assert.assertEquals(200, response.getStatus());
        
        Contentlet cont = APILocator.getContentletAPI().find(response.getHeaders().getFirst("inode"), sysuser, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        
        // must be in the first step
        Assert.assertEquals(step1.getId(), APILocator.getWorkflowAPI().findStepByContentlet(cont).getId());
        
        boolean assigned=false;
        
        HashMap<String, Object> map = new HashMap<String,Object>();
        map.put("assignedTo",billrole.getId());
        for(WorkflowTask task : APILocator.getWorkflowAPI().searchTasks(new WorkflowSearcher(map, sysuser))) {
            if(task.getWebasset().equals(cont.getIdentifier())) {
                assigned=true;
                Assert.assertEquals("please do this for me",task.getDescription());
                break;
            }
        }
        Assert.assertTrue(assigned);
        
    }
}










