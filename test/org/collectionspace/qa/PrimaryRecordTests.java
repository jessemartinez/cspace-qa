package org.collectionspace.qa;

import com.thoughtworks.selenium.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;
import static org.collectionspace.qa.Utilities.*;

@RunWith(value = Parameterized.class)
public class PrimaryRecordTests {

//    public static Record[] recordTypes;
    static Selenium selenium;
    public static String BASE_URL = "http://localhost:8180/collectionspace/ui/html/",
            LOGIN_URL = "index.html",
            LOGIN_USER = "admin@collectionspace.org",
            LOGIN_PASS = "Administrator",
            REDIRECT_URL = "myCollectionSpace.html",
            AFTER_DELETE_URL = "myCollectionSpace.html";
    private int primaryType;

    public PrimaryRecordTests(int number) {
        this.primaryType = number;
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
            //{Record.LOAN_OUT},
            {Record.ACQUISITION}
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void init() throws Exception {
        selenium = new DefaultSelenium("localhost", 8888, "firefox", BASE_URL);
        selenium.start();

        //log in:
        login(selenium);

    }

    /**
     * TEST: tests the creation of a new record via the create new page:
     * 1) Open create new page
     * 2) Select record in question via radio button
     * 3) Click Create
     *
     * X) Expect correct record page to be loaded
     * 
     * @throws Exception
     */
    @Test
    public void testCreateNew() throws Exception {
        log("CREATE NEW: Testing creating new " + Record.getRecordTypePP(primaryType) + "\n");
        selenium.open("createnew.html");
        textPresent(Record.getRecordTypePP(primaryType), selenium);
        selenium.click("css=:radio[value='" + Record.getRecordTypeShort(primaryType) + "']");
        selenium.click("//input[@value='Create']");
        log("CREATE NEW: expect correct record page to load and pattern chooser to show\n");
        waitForRecordLoad(selenium);
        assertEquals(Record.getRecordTypePP(primaryType), selenium.getText("css=#title-bar .record-type"));
    }

    /**
     * TEST: Tests the save functionality:
     * 1) Create a new record
     * 2) Fill out form with default values
     * 3) Save the record
     * 4) Check that the fields are as expected
     * 
     * @throws Exception
     */
    @Test
    public void testPrimarySave() throws Exception {
        //FIXME: OK retardo, dont hardcode this
        String primaryID = "standardID";

        log(Record.getRecordTypePP(primaryType) + ": test fill out record and save\n");
        selenium.open(Record.getRecordTypeShort(primaryType) + ".html");
        waitForRecordLoad(primaryType, selenium);

        fillForm(primaryType, primaryID, selenium);
        //save record
        log(Record.getRecordTypePP(primaryType) + ": expect save success message and that all fields are valid\n");
        save(selenium);
        //check values:
        verifyFill(primaryType, primaryID, selenium);
    }

    /**
     * TEST: Tests save functionality when the fields are empty
     *
     * PRE-REQUISITE: an already saved record is loaded
     * 1) Select the first option for all dropdowns
     * 2) Write the empty string in all fields
     * 3) Save
     * X) Expect no ID warning
     * 4) Fill out ID and save
     * X) Expect successful message
     * X) Expect all fields to be empty except for ID, Expect drop-downs to have index 0 selected
     * 
     * @throws Exception
     */
    @Test
    public void testRemovingValues() throws Exception {
        String primaryID = "standardID";
        log(Record.getRecordTypePP(primaryType) + ": test removing of values from fields and save\n");
        //Delete contents of all fields:
        clearForm(primaryType, selenium);
        //save record - and expect error due to missing ID
        selenium.click("//input[@value='Save']");
        //expect error message due to missing required field\n");
        elementPresent("CSS=.cs-message-error", selenium);
        assertEquals(Record.getRequiredIDmessage(primaryType), selenium.getText("CSS=.cs-message-error #message"));
        //Enter ID and save - expect successful
        selenium.type(Record.getIDSelector(primaryType), primaryID);
        selenium.click("//input[@value='Save']");
        textPresent("successfully", selenium);
        //check values:
        verifyClear(primaryType, selenium);
    }

    /**
     * TEST: test record deletion
     *
     * 1) Create new reocrd with a unique ID (based on timestamp)
     * 2) Save the record
     * 3) Click delete button
     * 4) Click cancel
     * 5) Click delete button
     * 6) Click close
     * 7) Click delete button and confirm
     * X) Expect successmessage
     * 8) Click OK on alert box telling successful delete
     * 9) Search for the record
     * X) Expect not found
     * 
     * @throws Exception
     */
    @Test
    public void testDeleteRecord() throws Exception {
        String uniqueID = Record.getRecordTypeShort(primaryType) + (new Date().getTime());
        createAndSave(primaryType, uniqueID, selenium);
        //test delete confirmation - Close and Cancel
        selenium.click("deleteButton");
        textPresent("Confirmation", selenium);
        assertTrue(selenium.isTextPresent("exact:Delete this record?"));
        selenium.click("//img[@alt='close dialog']");
        selenium.click("deleteButton");
        selenium.click("//input[@value='Cancel']");
        selenium.click("deleteButton");
        //Test  successfull delete
        selenium.click("css=.cs-confirmationDialog :input[value='Delete']");
        textPresent("Record successfully deleted", selenium);
        selenium.click("css=.cs-confirmationDialog :input[value='OK']");
        selenium.waitForPageToLoad(MAX_WAIT);
        //expect redirect to AFTER_DELETE_URL 
        assertEquals(BASE_URL + AFTER_DELETE_URL, selenium.getLocation());
        //check that the record is indeed deleted
        elementPresent("css=.cs-searchBox :input[value='Search']", selenium);
        selenium.select("recordTypeSelect-selection", "label=" + Record.getRecordTypePP(primaryType));
        selenium.type("css=.cs-searchBox :input[name='query']", uniqueID);
        selenium.click("css=.cs-searchBox :input[value='Search']");
        selenium.waitForPageToLoad(MAX_WAIT);
        //expect no results when searching for the record\n");
        textPresent("Found 0 records for " + uniqueID, selenium);
        assertFalse(selenium.isElementPresent("link="+uniqueID));
    }
}