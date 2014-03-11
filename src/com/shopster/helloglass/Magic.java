/*
 * Magic.java
 * 
 * This is the service which is started from HelloGlass.java, this is where the magic happens.
 */
package com.shopster.helloglass;

import com.google.android.glass.app.Card;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.List;

public class Magic extends Activity {
	

	private static final int REQUEST_CODE = 1234;
	private Card card1;
	private GestureDetector mGestureDetector;
	private static final int ITEMS_PER_PAGE = 2;	
	
	
  class ShoppingList {
	  public String name;
	  public List<String> shoppingList = new ArrayList<String>();
	  public int curPage = 0; //which page of the list we're on
	  
	  public int numPages() {
		  if (shoppingList.size() == 0) return 1;
		  return (shoppingList.size() - 1) / ITEMS_PER_PAGE + 1;
	  }
	  
	  public String listString() {
		  return textForPage(curPage);
	  }
	  
	  private String textForPage(int pageNum) {
		  String pageString = "";
		  boolean firstItem = true;
		  int firstIndex = pageNum * ITEMS_PER_PAGE;
		  for (int i = firstIndex; i < firstIndex + ITEMS_PER_PAGE; i++) {
			  if (i >= shoppingList.size()) break;
			  if (!firstItem) {
				  pageString += "\n";
			  }
			  pageString += shoppingList.get(i);
			  firstItem = false; 
		  }
		  return pageString;
	  }
	  
  }
  
  private ShoppingList curList = new ShoppingList();
  private List<ShoppingList> shoppingLists = new ArrayList<ShoppingList>();
  private boolean listsMode = true;
  
  public String listsString() {
	  String pageString = "";
	  boolean firstItem = true;
	  for (int i = 0; i < shoppingLists.size(); i++) {
		  if (!firstItem) {
			  pageString += "\n";
		  }
		  pageString += shoppingLists.get(i).name;
		  firstItem = false; 
	  }
	  return pageString;
  }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * We're creating a card for the interface.
		 * 
		 * More info here: http://developer.android.com/guide/topics/ui/themes.html
		 */
	card1 = new Card(this);
    mGestureDetector = createGestureDetector(this);


    card1.setText("Tap to create list"); // Main text area
    card1.setFootnote("Shopping lists");

    // Alert user if no recognition service is present.
    PackageManager pm = getPackageManager();
    List<ResolveInfo> activities = pm.queryIntentActivities(
        new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
    if (activities.size() == 0)
    {
      card1.setText("RECOGNIZER NOT PRESENT");
    }
		View card1View = card1.toView();
		
		// Display the card we just created
		setContentView(card1View);
	}

  private void updateCard() {
	Log.d("UpdatingCard", Boolean.toString(listsMode));
	if (!listsMode) {
	  	card1.setText(curList.listString()); // Main text area
	    card1.setFootnote(curList.name + " (page " + Integer.toString(curList.curPage + 1) + "/" + Integer.toString(curList.numPages()) + ")");
	}
	else {
		card1.setText(listsString()); // Main text area
	    card1.setFootnote("Shopping lists");
	}
	View card1View = card1.toView();
    // Display the card we just created
    setContentView(card1View);
  }
  
  private GestureDetector createGestureDetector(Context context)
  {
    GestureDetector gestureDetector = new GestureDetector(context);
    // Create a base listener for generic gestures.
    gestureDetector.setBaseListener(new GestureDetector.BaseListener()
    {
      @Override
      public boolean onGesture(Gesture gesture)
      {
        if (gesture == Gesture.TAP)
        {
          // do something on tap
          // Such as creating a new card and putting some info into it.
          //card1.setText("TAPPED GLASS"); // Main text area
          View card1View = card1.toView();
      
          // Display the card we just created
          setContentView(card1View);
          startVoiceRecognitionActivity(false);
          return true;
        }
        else if (gesture == Gesture.SWIPE_LEFT) {
        	curList.curPage = (curList.curPage - 1 + curList.numPages()) % curList.numPages();
        	updateCard();
            return true;
        }
        else if (gesture == Gesture.SWIPE_RIGHT) {
        	curList.curPage = (curList.curPage + 1) % curList.numPages();
        	updateCard();
            return true;
        }
        return false;
      }
    });

    return gestureDetector;
  }
  
  /**
   * Send generic motion events to the gesture detector.
   */
  public boolean onGenericMotionEvent(MotionEvent event)
  {
     if (mGestureDetector != null)
     {
    	 return mGestureDetector.onMotionEvent(event);
     }
     return false;
  }

  /**
   * Fire an intent to start the voice recognition activity.
   */
  private void startVoiceRecognitionActivity(boolean isRetry)
  {
	String prompt;
	if (listsMode) {
		if (isRetry) prompt = "Command not recognized, say start/open/delete (list)";
		else prompt = "Say start/open/delete (list)";
	}
	else {
		if (isRetry) prompt = "Command not recognized, say add/remove (item) or back";
		else prompt = "Say add/remove (item) or back";
	}
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
    startActivityForResult(intent, REQUEST_CODE);
  }

  /**
   * Handle the results from the voice recognition activity.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
    {
      // Populate the card with the String value of the highest confidence rating which the recognition engine thought it heard.
      ArrayList<String> matches = data.getStringArrayListExtra(
          RecognizerIntent.EXTRA_RESULTS);
      // Get first match (which has highest confidence value)
      Log.d("Recognitions", matches.toString());
      
      String bestMatch = matches.get(0);
      boolean validInput = false;

	  if (bestMatch.equals("back")) {
		  listsMode = true;
		  validInput = true;
	  }
	  
      int spacePos = bestMatch.indexOf(' ');
      if (spacePos != -1) {
    	  String command = bestMatch.substring(0,spacePos);
    	  String item = bestMatch.substring(spacePos + 1);
    	  Log.d("Command", command);
    	  
    	if(!listsMode) {
    	  //assume "and" is a misrecognized "add"
    	  if (command.equals("add") || command.equals("and")) {
    		  //max 6 items per page apparently
    		  curList.shoppingList.add(item);
    		  validInput = true;
    	  }
    	  else if (command.equals("delete") || command.equals("remove")) {
    		  /*for (String temp : shoppingList) {
    			  if (temp.equals(item))
    				  shoppingList.remove(temp);
    		  }*/
    		  curList.shoppingList.remove(item);
    		  //Make sure we don't stay on an empty page
    		  if (curList.curPage >= curList.numPages()) curList.curPage = curList.numPages() - 1;
    		  validInput = true;
    	  }
    	  
    	}
    	else {
    		if (command.equals("start") || command.equals("create")) {
      		  //max 6 items per page apparently
    		  ShoppingList newList = new ShoppingList();
    		  newList.name = item;
      		  shoppingLists.add(newList);
      		  validInput = true;
      	  }
      	  else if (command.equals("delete") || command.equals("remove")) {
      		  for (int i = 0; i < shoppingLists.size(); i++) {
      			  if (shoppingLists.get(i).name.equals(item)) {
      				  shoppingLists.remove(i);
      				  break;
      			  }
      		  }
      		  validInput = true;
      	  }
      	  else if (command.equals("open")) {
      		for (int i = 0; i < shoppingLists.size(); i++) {
    			  if (shoppingLists.get(i).name.equals(item)) {
    				  curList = shoppingLists.get(i);
    				  listsMode = false;
    				  break;
    			  }
    		  }
    		  validInput = true;
    	  }
    	}
      }
      if (!validInput) {
		  startVoiceRecognitionActivity(true);
		  return;
	  }
      updateCard();
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

}
