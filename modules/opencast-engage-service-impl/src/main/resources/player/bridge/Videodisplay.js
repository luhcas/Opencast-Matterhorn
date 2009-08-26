/**
 * WARNING! THIS IS A GENERATED FILE, AND WILL BE RE-GENERATED EACH TIME THE
 * AJAXBRIDGE IS RUN.
 *
 * You should keep your javascript code inside this file as light as possible, 
 * and rather keep the body of your Ajax application in separate *.js files. 
 *
 * Do make a backup of your changes, before re-generating this file (AjaxBridge 
 * will display a warning message to you).
 *
 * Please refer to the built-in documentation inside the AjaxBridge application 
 * for help on using this file.
 */
 
 
/**
 * Application "Videodisplay.mxml"
 */

/**
 * The "Videodisplay" javascript namespace. All the functions/variables you
 * have selected under the "Videodisplay.mxml" in the tree will be
 * available as static members of this namespace object.
 */
Videodisplay = {};


/**
 * Listen for the instantiation of the Flex application over the bridge
 */
FABridge.addInitializationCallback("b_Videodisplay", VideodisplayReady);


/**
 * Hook here all the code that must run as soon as the "Videodisplay" class
 * finishes its instantiation over the bridge.
 *
 * For basic tasks, such as running a Flex method on the click of a javascript
 * button, chances are that both Ajax and Flex may well have loaded before the 
 * user actually clicks the button.
 *
 * However, using the "VideodisplayReady()" is the safest way, as it will 
 * let Ajax know that involved Flex classes are available for use.
 */
function VideodisplayReady() {

	// Initialize the "root" object. This represents the actual 
	// "Videodisplay.mxml" flex application.
	b_Videodisplay_root = FABridge["b_Videodisplay"].root();
	

	// Global variables in the "Videodisplay.mxml" application (converted 
	// to getters and setters)

	Videodisplay.getModelVideoplayer = function () {
		return b_Videodisplay_root.getModelVideoplayer();
	};


	Videodisplay.getDisplay = function () {
		return b_Videodisplay_root.getDisplay();
	};


	Videodisplay.getTransitions = function () {
		return b_Videodisplay_root.getTransitions();
	};


	Videodisplay.getStates = function () {
		return b_Videodisplay_root.getStates();
	};


	Videodisplay.getControlBar = function () {
		return b_Videodisplay_root.getControlBar();
	};


	Videodisplay.getPageTitle = function () {
		return b_Videodisplay_root.getPageTitle();
	};


	Videodisplay.getPreloader = function () {
		return b_Videodisplay_root.getPreloader();
	};


	Videodisplay.getScriptTimeLimit = function () {
		return b_Videodisplay_root.getScriptTimeLimit();
	};


	Videodisplay.getResetHistory = function () {
		return b_Videodisplay_root.getResetHistory();
	};


	Videodisplay.getFrameRate = function () {
		return b_Videodisplay_root.getFrameRate();
	};


	Videodisplay.getScriptRecursionLimit = function () {
		return b_Videodisplay_root.getScriptRecursionLimit();
	};


	Videodisplay.getHistoryManagementEnabled = function () {
		return b_Videodisplay_root.getHistoryManagementEnabled();
	};


	Videodisplay.getUsePreloader = function () {
		return b_Videodisplay_root.getUsePreloader();
	};


	Videodisplay.getConstructor = function () {
		return b_Videodisplay_root.getConstructor();
	};


	Videodisplay.getSuper = function () {
		return b_Videodisplay_root.getSuper();
	};


	Videodisplay.getThis = function () {
		return b_Videodisplay_root.getThis();
	};


	// Global functions in the "Videodisplay.mxml" application

	Videodisplay.play = function() {
		b_Videodisplay_root.play();
	};

	Videodisplay.stop = function() {
		b_Videodisplay_root.stop();
	};

	Videodisplay.pause = function() {
		b_Videodisplay_root.pause();
	};

	Videodisplay.regenerateStyleCache = function(argBoolean) {
		b_Videodisplay_root.regenerateStyleCache(argBoolean);
	};

	Videodisplay.initialize = function() {
		b_Videodisplay_root.initialize();
	};

	Videodisplay.getAutomationTabularData = function() {
		return b_Videodisplay_root.getAutomationTabularData();
	};

	Videodisplay.getUid = function() {
		return b_Videodisplay_root.getUid();
	};

	Videodisplay.setUid = function(argString) {
		b_Videodisplay_root.setUid(argString);
	};

	Videodisplay.getScaleY = function() {
		return b_Videodisplay_root.getScaleY();
	};

	Videodisplay.setScaleY = function(argNumber) {
		b_Videodisplay_root.setScaleY(argNumber);
	};

	Videodisplay.getScaleX = function() {
		return b_Videodisplay_root.getScaleX();
	};

	Videodisplay.setScaleX = function(argNumber) {
		b_Videodisplay_root.setScaleX(argNumber);
	};

	Videodisplay.getRepeaterItem = function(argInt) {
		return b_Videodisplay_root.getRepeaterItem(argInt);
	};

	Videodisplay.getStyleDeclaration = function() {
		return b_Videodisplay_root.getStyleDeclaration();
	};

	Videodisplay.setStyleDeclaration = function(argCSSStyleDeclaration) {
		b_Videodisplay_root.setStyleDeclaration(argCSSStyleDeclaration);
	};

	Videodisplay.getMaxWidth = function() {
		return b_Videodisplay_root.getMaxWidth();
	};

	Videodisplay.setMaxWidth = function(argNumber) {
		b_Videodisplay_root.setMaxWidth(argNumber);
	};

	Videodisplay.measureHTMLText = function(argString) {
		return b_Videodisplay_root.measureHTMLText(argString);
	};

	Videodisplay.getSystemManager = function() {
		return b_Videodisplay_root.getSystemManager();
	};

	Videodisplay.setSystemManager = function(argISystemManager) {
		b_Videodisplay_root.setSystemManager(argISystemManager);
	};

	Videodisplay.validateDisplayList = function() {
		b_Videodisplay_root.validateDisplayList();
	};

	Videodisplay.getMinWidth = function() {
		return b_Videodisplay_root.getMinWidth();
	};

	Videodisplay.setMinWidth = function(argNumber) {
		b_Videodisplay_root.setMinWidth(argNumber);
	};

	Videodisplay.getExplicitOrMeasuredWidth = function() {
		return b_Videodisplay_root.getExplicitOrMeasuredWidth();
	};

	Videodisplay.getInitialized = function() {
		return b_Videodisplay_root.getInitialized();
	};

	Videodisplay.setInitialized = function(argBoolean) {
		b_Videodisplay_root.setInitialized(argBoolean);
	};

	Videodisplay.contentToGlobal = function(argPoint) {
		return b_Videodisplay_root.contentToGlobal(argPoint);
	};

	Videodisplay.getAutomationValue = function() {
		return b_Videodisplay_root.getAutomationValue();
	};

	Videodisplay.getExplicitHeight = function() {
		return b_Videodisplay_root.getExplicitHeight();
	};

	Videodisplay.setExplicitHeight = function(argNumber) {
		b_Videodisplay_root.setExplicitHeight(argNumber);
	};

	Videodisplay.executeBindings = function(argBoolean) {
		b_Videodisplay_root.executeBindings(argBoolean);
	};

	Videodisplay.getPercentWidth = function() {
		return b_Videodisplay_root.getPercentWidth();
	};

	Videodisplay.setPercentWidth = function(argNumber) {
		b_Videodisplay_root.setPercentWidth(argNumber);
	};

	Videodisplay.getModuleFactory = function() {
		return b_Videodisplay_root.getModuleFactory();
	};

	Videodisplay.setModuleFactory = function(argIFlexModuleFactory) {
		b_Videodisplay_root.setModuleFactory(argIFlexModuleFactory);
	};

	Videodisplay.getParentApplication = function() {
		return b_Videodisplay_root.getParentApplication();
	};

	Videodisplay.drawRoundRect = function(argNumber1, argNumber2, argNumber3, argNumber4, argObject5, argObject6, argObject7, argObject8, argString, argArray, argObject9) {
		b_Videodisplay_root.drawRoundRect(argNumber1, argNumber2, argNumber3, argNumber4, argObject5, argObject6, argObject7, argObject8, argString, argArray, argObject9);
	};

	Videodisplay.resolveAutomationIDPart = function(argObject) {
		return b_Videodisplay_root.resolveAutomationIDPart(argObject);
	};

	Videodisplay.setChildIndex = function(argDisplayObject, argInt) {
		b_Videodisplay_root.setChildIndex(argDisplayObject, argInt);
	};

	Videodisplay.getUpdateCompletePendingFlag = function() {
		return b_Videodisplay_root.getUpdateCompletePendingFlag();
	};

	Videodisplay.setUpdateCompletePendingFlag = function(argBoolean) {
		b_Videodisplay_root.setUpdateCompletePendingFlag(argBoolean);
	};

	Videodisplay.getProcessedDescriptors = function() {
		return b_Videodisplay_root.getProcessedDescriptors();
	};

	Videodisplay.setProcessedDescriptors = function(argBoolean) {
		b_Videodisplay_root.setProcessedDescriptors(argBoolean);
	};

	Videodisplay.getDoubleClickEnabled = function() {
		return b_Videodisplay_root.getDoubleClickEnabled();
	};

	Videodisplay.setDoubleClickEnabled = function(argBoolean) {
		b_Videodisplay_root.setDoubleClickEnabled(argBoolean);
	};

	Videodisplay.setActualSize = function(argNumber1, argNumber2) {
		b_Videodisplay_root.setActualSize(argNumber1, argNumber2);
	};

	Videodisplay.getOwner = function() {
		return b_Videodisplay_root.getOwner();
	};

	Videodisplay.setOwner = function(argDisplayObjectContainer) {
		b_Videodisplay_root.setOwner(argDisplayObjectContainer);
	};

	Videodisplay.measureText = function(argString) {
		return b_Videodisplay_root.measureText(argString);
	};

	Videodisplay.getRepeaters = function() {
		return b_Videodisplay_root.getRepeaters();
	};

	Videodisplay.setRepeaters = function(argArray) {
		b_Videodisplay_root.setRepeaters(argArray);
	};

	Videodisplay.notifyStyleChangeInChildren = function(argString, argBoolean) {
		b_Videodisplay_root.notifyStyleChangeInChildren(argString, argBoolean);
	};

	Videodisplay.setStyle = function(argString, argObject) {
		b_Videodisplay_root.setStyle(argString, argObject);
	};

	Videodisplay.getFlexContextMenu = function() {
		return b_Videodisplay_root.getFlexContextMenu();
	};

	Videodisplay.setFlexContextMenu = function(argIFlexContextMenu) {
		b_Videodisplay_root.setFlexContextMenu(argIFlexContextMenu);
	};

	Videodisplay.createReferenceOnParentDocument = function(argIFlexDisplayObject) {
		b_Videodisplay_root.createReferenceOnParentDocument(argIFlexDisplayObject);
	};

	Videodisplay.getMouseFocusEnabled = function() {
		return b_Videodisplay_root.getMouseFocusEnabled();
	};

	Videodisplay.setMouseFocusEnabled = function(argBoolean) {
		b_Videodisplay_root.setMouseFocusEnabled(argBoolean);
	};

	Videodisplay.stopDrag = function() {
		b_Videodisplay_root.stopDrag();
	};

	Videodisplay.localToContent = function(argPoint) {
		return b_Videodisplay_root.localToContent(argPoint);
	};

	Videodisplay.prepareToPrint = function(argIFlexDisplayObject) {
		return b_Videodisplay_root.prepareToPrint(argIFlexDisplayObject);
	};

	Videodisplay.endEffectsStarted = function() {
		b_Videodisplay_root.endEffectsStarted();
	};

	Videodisplay.registerEffects = function(argArray) {
		b_Videodisplay_root.registerEffects(argArray);
	};

	Videodisplay.getActiveEffects = function() {
		return b_Videodisplay_root.getActiveEffects();
	};

	Videodisplay.getFocusPane = function() {
		return b_Videodisplay_root.getFocusPane();
	};

	Videodisplay.setFocusPane = function(argSprite) {
		b_Videodisplay_root.setFocusPane(argSprite);
	};

	Videodisplay.getInheritingStyles = function() {
		return b_Videodisplay_root.getInheritingStyles();
	};

	Videodisplay.setInheritingStyles = function(argObject) {
		b_Videodisplay_root.setInheritingStyles(argObject);
	};

	Videodisplay.verticalGradientMatrix = function(argNumber1, argNumber2, argNumber3, argNumber4) {
		return b_Videodisplay_root.verticalGradientMatrix(argNumber1, argNumber2, argNumber3, argNumber4);
	};

	Videodisplay.determineTextFormatFromStyles = function() {
		return b_Videodisplay_root.determineTextFormatFromStyles();
	};

	Videodisplay.getMaxHeight = function() {
		return b_Videodisplay_root.getMaxHeight();
	};

	Videodisplay.setMaxHeight = function(argNumber) {
		b_Videodisplay_root.setMaxHeight(argNumber);
	};

	Videodisplay.getBaselinePosition = function() {
		return b_Videodisplay_root.getBaselinePosition();
	};

	Videodisplay.callLater = function(argFunction, argArray) {
		b_Videodisplay_root.callLater(argFunction, argArray);
	};

	Videodisplay.hasFontContextChanged = function() {
		return b_Videodisplay_root.hasFontContextChanged();
	};

	Videodisplay.getDescriptor = function() {
		return b_Videodisplay_root.getDescriptor();
	};

	Videodisplay.setDescriptor = function(argUIComponentDescriptor) {
		b_Videodisplay_root.setDescriptor(argUIComponentDescriptor);
	};

	Videodisplay.deleteReferenceOnParentDocument = function(argIFlexDisplayObject) {
		b_Videodisplay_root.deleteReferenceOnParentDocument(argIFlexDisplayObject);
	};

	Videodisplay.getErrorString = function() {
		return b_Videodisplay_root.getErrorString();
	};

	Videodisplay.setErrorString = function(argString) {
		b_Videodisplay_root.setErrorString(argString);
	};

	Videodisplay.getWidth = function() {
		return b_Videodisplay_root.getWidth();
	};

	Videodisplay.setWidth = function(argNumber) {
		b_Videodisplay_root.setWidth(argNumber);
	};

	Videodisplay.getInstanceIndex = function() {
		return b_Videodisplay_root.getInstanceIndex();
	};

	Videodisplay.move = function(argNumber1, argNumber2) {
		b_Videodisplay_root.move(argNumber1, argNumber2);
	};

	Videodisplay.getClassStyleDeclarations = function() {
		return b_Videodisplay_root.getClassStyleDeclarations();
	};

	Videodisplay.initializeRepeaterArrays = function(argIRepeaterClient) {
		b_Videodisplay_root.initializeRepeaterArrays(argIRepeaterClient);
	};

	Videodisplay.getExplicitMaxWidth = function() {
		return b_Videodisplay_root.getExplicitMaxWidth();
	};

	Videodisplay.setExplicitMaxWidth = function(argNumber) {
		b_Videodisplay_root.setExplicitMaxWidth(argNumber);
	};

	Videodisplay.getExplicitMinHeight = function() {
		return b_Videodisplay_root.getExplicitMinHeight();
	};

	Videodisplay.setExplicitMinHeight = function(argNumber) {
		b_Videodisplay_root.setExplicitMinHeight(argNumber);
	};

	Videodisplay.clearStyle = function(argString) {
		b_Videodisplay_root.clearStyle(argString);
	};

	Videodisplay.invalidateProperties = function() {
		b_Videodisplay_root.invalidateProperties();
	};

	Videodisplay.setCacheHeuristic = function(argBoolean) {
		b_Videodisplay_root.setCacheHeuristic(argBoolean);
	};

	Videodisplay.getFilters = function() {
		return b_Videodisplay_root.getFilters();
	};

	Videodisplay.setFilters = function(argArray) {
		b_Videodisplay_root.setFilters(argArray);
	};

	Videodisplay.validateProperties = function() {
		b_Videodisplay_root.validateProperties();
	};

	Videodisplay.getIncludeInLayout = function() {
		return b_Videodisplay_root.getIncludeInLayout();
	};

	Videodisplay.setIncludeInLayout = function(argBoolean) {
		b_Videodisplay_root.setIncludeInLayout(argBoolean);
	};

	Videodisplay.addChildAt = function(argDisplayObject, argInt) {
		return b_Videodisplay_root.addChildAt(argDisplayObject, argInt);
	};

	Videodisplay.getAutomationName = function() {
		return b_Videodisplay_root.getAutomationName();
	};

	Videodisplay.setAutomationName = function(argString) {
		b_Videodisplay_root.setAutomationName(argString);
	};

	Videodisplay.getClassName = function() {
		return b_Videodisplay_root.getClassName();
	};

	Videodisplay.getNonInheritingStyles = function() {
		return b_Videodisplay_root.getNonInheritingStyles();
	};

	Videodisplay.setNonInheritingStyles = function(argObject) {
		b_Videodisplay_root.setNonInheritingStyles(argObject);
	};

	Videodisplay.getExplicitWidth = function() {
		return b_Videodisplay_root.getExplicitWidth();
	};

	Videodisplay.setExplicitWidth = function(argNumber) {
		b_Videodisplay_root.setExplicitWidth(argNumber);
	};

	Videodisplay.getMinHeight = function() {
		return b_Videodisplay_root.getMinHeight();
	};

	Videodisplay.setMinHeight = function(argNumber) {
		b_Videodisplay_root.setMinHeight(argNumber);
	};

	Videodisplay.dispatchEvent = function(argEvent) {
		return b_Videodisplay_root.dispatchEvent(argEvent);
	};

	Videodisplay.getExplicitMinWidth = function() {
		return b_Videodisplay_root.getExplicitMinWidth();
	};

	Videodisplay.setExplicitMinWidth = function(argNumber) {
		b_Videodisplay_root.setExplicitMinWidth(argNumber);
	};

	Videodisplay.getStyle = function(argString) {
		return b_Videodisplay_root.getStyle(argString);
	};

	Videodisplay.getMouseY = function() {
		return b_Videodisplay_root.getMouseY();
	};

	Videodisplay.getMouseX = function() {
		return b_Videodisplay_root.getMouseX();
	};

	Videodisplay.getScreen = function() {
		return b_Videodisplay_root.getScreen();
	};

	Videodisplay.getExplicitOrMeasuredHeight = function() {
		return b_Videodisplay_root.getExplicitOrMeasuredHeight();
	};

	Videodisplay.setFocus = function() {
		b_Videodisplay_root.setFocus();
	};

	Videodisplay.horizontalGradientMatrix = function(argNumber1, argNumber2, argNumber3, argNumber4) {
		return b_Videodisplay_root.horizontalGradientMatrix(argNumber1, argNumber2, argNumber3, argNumber4);
	};

	Videodisplay.setConstraintValue = function(argString, argObject) {
		b_Videodisplay_root.setConstraintValue(argString, argObject);
	};

	Videodisplay.getInstanceIndices = function() {
		return b_Videodisplay_root.getInstanceIndices();
	};

	Videodisplay.setInstanceIndices = function(argArray) {
		b_Videodisplay_root.setInstanceIndices(argArray);
	};

	Videodisplay.getRepeaterIndices = function() {
		return b_Videodisplay_root.getRepeaterIndices();
	};

	Videodisplay.setRepeaterIndices = function(argArray) {
		b_Videodisplay_root.setRepeaterIndices(argArray);
	};

	Videodisplay.getTweeningProperties = function() {
		return b_Videodisplay_root.getTweeningProperties();
	};

	Videodisplay.setTweeningProperties = function(argArray) {
		b_Videodisplay_root.setTweeningProperties(argArray);
	};

	Videodisplay.getCachePolicy = function() {
		return b_Videodisplay_root.getCachePolicy();
	};

	Videodisplay.setCachePolicy = function(argString) {
		b_Videodisplay_root.setCachePolicy(argString);
	};

	Videodisplay.addChild = function(argDisplayObject) {
		return b_Videodisplay_root.addChild(argDisplayObject);
	};

	Videodisplay.invalidateSize = function() {
		b_Videodisplay_root.invalidateSize();
	};

	Videodisplay.setVisible = function(argBoolean1, argBoolean2) {
		b_Videodisplay_root.setVisible(argBoolean1, argBoolean2);
	};

	Videodisplay.parentChanged = function(argDisplayObjectContainer) {
		b_Videodisplay_root.parentChanged(argDisplayObjectContainer);
	};

	Videodisplay.getMeasuredHeight = function() {
		return b_Videodisplay_root.getMeasuredHeight();
	};

	Videodisplay.setMeasuredHeight = function(argNumber) {
		b_Videodisplay_root.setMeasuredHeight(argNumber);
	};

	Videodisplay.removeChild = function(argDisplayObject) {
		return b_Videodisplay_root.removeChild(argDisplayObject);
	};

	Videodisplay.validateNow = function() {
		b_Videodisplay_root.validateNow();
	};

	Videodisplay.invalidateDisplayList = function() {
		b_Videodisplay_root.invalidateDisplayList();
	};

	Videodisplay.getMeasuredWidth = function() {
		return b_Videodisplay_root.getMeasuredWidth();
	};

	Videodisplay.setMeasuredWidth = function(argNumber) {
		b_Videodisplay_root.setMeasuredWidth(argNumber);
	};

	Videodisplay.getAutomationChildAt = function(argInt) {
		return b_Videodisplay_root.getAutomationChildAt(argInt);
	};

	Videodisplay.getPercentHeight = function() {
		return b_Videodisplay_root.getPercentHeight();
	};

	Videodisplay.setPercentHeight = function(argNumber) {
		b_Videodisplay_root.setPercentHeight(argNumber);
	};

	Videodisplay.getIsPopUp = function() {
		return b_Videodisplay_root.getIsPopUp();
	};

	Videodisplay.setIsPopUp = function(argBoolean) {
		b_Videodisplay_root.setIsPopUp(argBoolean);
	};

	Videodisplay.getId = function() {
		return b_Videodisplay_root.getId();
	};

	Videodisplay.setId = function(argString) {
		b_Videodisplay_root.setId(argString);
	};

	Videodisplay.getStyleName = function() {
		return b_Videodisplay_root.getStyleName();
	};

	Videodisplay.setStyleName = function(argObject) {
		b_Videodisplay_root.setStyleName(argObject);
	};

	Videodisplay.globalToContent = function(argPoint) {
		return b_Videodisplay_root.globalToContent(argPoint);
	};

	Videodisplay.getIsDocument = function() {
		return b_Videodisplay_root.getIsDocument();
	};

	Videodisplay.setCacheAsBitmap = function(argBoolean) {
		b_Videodisplay_root.setCacheAsBitmap(argBoolean);
	};

	Videodisplay.getRepeaterIndex = function() {
		return b_Videodisplay_root.getRepeaterIndex();
	};

	Videodisplay.getParent = function() {
		return b_Videodisplay_root.getParent();
	};

	Videodisplay.getRepeater = function() {
		return b_Videodisplay_root.getRepeater();
	};

	Videodisplay.getMeasuredMinHeight = function() {
		return b_Videodisplay_root.getMeasuredMinHeight();
	};

	Videodisplay.setMeasuredMinHeight = function(argNumber) {
		b_Videodisplay_root.setMeasuredMinHeight(argNumber);
	};

	Videodisplay.getVisibleRect = function(argDisplayObject) {
		return b_Videodisplay_root.getVisibleRect(argDisplayObject);
	};

	Videodisplay.getFocusManager = function() {
		return b_Videodisplay_root.getFocusManager();
	};

	Videodisplay.setFocusManager = function(argIFocusManager) {
		b_Videodisplay_root.setFocusManager(argIFocusManager);
	};

	Videodisplay.effectStarted = function(argIEffectInstance) {
		b_Videodisplay_root.effectStarted(argIEffectInstance);
	};

	Videodisplay.UIComponent = function() {
		return b_Videodisplay_root.UIComponent();
	};

	Videodisplay.getDocument = function() {
		return b_Videodisplay_root.getDocument();
	};

	Videodisplay.setDocument = function(argObject) {
		b_Videodisplay_root.setDocument(argObject);
	};

	Videodisplay.getFocus = function() {
		return b_Videodisplay_root.getFocus();
	};

	Videodisplay.validationResultHandler = function(argValidationResultEvent) {
		b_Videodisplay_root.validationResultHandler(argValidationResultEvent);
	};

	Videodisplay.setCurrentState = function(argString, argBoolean) {
		b_Videodisplay_root.setCurrentState(argString, argBoolean);
	};

	Videodisplay.finishPrint = function(argObject, argIFlexDisplayObject) {
		b_Videodisplay_root.finishPrint(argObject, argIFlexDisplayObject);
	};

	Videodisplay.contentToLocal = function(argPoint) {
		return b_Videodisplay_root.contentToLocal(argPoint);
	};

	Videodisplay.validateSize = function(argBoolean) {
		b_Videodisplay_root.validateSize(argBoolean);
	};

	Videodisplay.getEnabled = function() {
		return b_Videodisplay_root.getEnabled();
	};

	Videodisplay.setEnabled = function(argBoolean) {
		b_Videodisplay_root.setEnabled(argBoolean);
	};

	Videodisplay.getNestLevel = function() {
		return b_Videodisplay_root.getNestLevel();
	};

	Videodisplay.setNestLevel = function(argInt) {
		b_Videodisplay_root.setNestLevel(argInt);
	};

	Videodisplay.getCursorManager = function() {
		return b_Videodisplay_root.getCursorManager();
	};

	Videodisplay.getValidationSubField = function() {
		return b_Videodisplay_root.getValidationSubField();
	};

	Videodisplay.setValidationSubField = function(argString) {
		b_Videodisplay_root.setValidationSubField(argString);
	};

	Videodisplay.setAlpha = function(argNumber) {
		b_Videodisplay_root.setAlpha(argNumber);
	};

	Videodisplay.styleChanged = function(argString) {
		b_Videodisplay_root.styleChanged(argString);
	};

	Videodisplay.getVisible = function() {
		return b_Videodisplay_root.getVisible();
	};

	Videodisplay.setVisible = function(argBoolean) {
		b_Videodisplay_root.setVisible(argBoolean);
	};

	Videodisplay.getHeight = function() {
		return b_Videodisplay_root.getHeight();
	};

	Videodisplay.setHeight = function(argNumber) {
		b_Videodisplay_root.setHeight(argNumber);
	};

	Videodisplay.removeChildAt = function(argInt) {
		return b_Videodisplay_root.removeChildAt(argInt);
	};

	Videodisplay.getY = function() {
		return b_Videodisplay_root.getY();
	};

	Videodisplay.setY = function(argNumber) {
		b_Videodisplay_root.setY(argNumber);
	};

	Videodisplay.getX = function() {
		return b_Videodisplay_root.getX();
	};

	Videodisplay.setX = function(argNumber) {
		b_Videodisplay_root.setX(argNumber);
	};

	Videodisplay.getAutomationDelegate = function() {
		return b_Videodisplay_root.getAutomationDelegate();
	};

	Videodisplay.setAutomationDelegate = function(argObject) {
		b_Videodisplay_root.setAutomationDelegate(argObject);
	};

	Videodisplay.replayAutomatableEvent = function(argEvent) {
		return b_Videodisplay_root.replayAutomatableEvent(argEvent);
	};

	Videodisplay.getConstraintValue = function(argString) {
		return b_Videodisplay_root.getConstraintValue(argString);
	};

	Videodisplay.getMeasuredMinWidth = function() {
		return b_Videodisplay_root.getMeasuredMinWidth();
	};

	Videodisplay.setMeasuredMinWidth = function(argNumber) {
		b_Videodisplay_root.setMeasuredMinWidth(argNumber);
	};

	Videodisplay.getToolTip = function() {
		return b_Videodisplay_root.getToolTip();
	};

	Videodisplay.setToolTip = function(argString) {
		b_Videodisplay_root.setToolTip(argString);
	};

	Videodisplay.getNumAutomationChildren = function() {
		return b_Videodisplay_root.getNumAutomationChildren();
	};

	Videodisplay.getParentDocument = function() {
		return b_Videodisplay_root.getParentDocument();
	};

	Videodisplay.stylesInitialized = function() {
		b_Videodisplay_root.stylesInitialized();
	};

	Videodisplay.effectFinished = function(argIEffectInstance) {
		b_Videodisplay_root.effectFinished(argIEffectInstance);
	};

	Videodisplay.getContentMouseY = function() {
		return b_Videodisplay_root.getContentMouseY();
	};

	Videodisplay.getContentMouseX = function() {
		return b_Videodisplay_root.getContentMouseX();
	};

	Videodisplay.getExplicitMaxHeight = function() {
		return b_Videodisplay_root.getExplicitMaxHeight();
	};

	Videodisplay.setExplicitMaxHeight = function(argNumber) {
		b_Videodisplay_root.setExplicitMaxHeight(argNumber);
	};

	Videodisplay.createAutomationIDPart = function(argIAutomationObject) {
		return b_Videodisplay_root.createAutomationIDPart(argIAutomationObject);
	};

	Videodisplay.getCurrentState = function() {
		return b_Videodisplay_root.getCurrentState();
	};

	Videodisplay.setCurrentState = function(argString) {
		b_Videodisplay_root.setCurrentState(argString);
	};

	Videodisplay.owns = function(argDisplayObject) {
		return b_Videodisplay_root.owns(argDisplayObject);
	};

	Videodisplay.getShowInAutomationHierarchy = function() {
		return b_Videodisplay_root.getShowInAutomationHierarchy();
	};

	Videodisplay.setShowInAutomationHierarchy = function(argBoolean) {
		b_Videodisplay_root.setShowInAutomationHierarchy(argBoolean);
	};

	Videodisplay.drawFocus = function(argBoolean) {
		b_Videodisplay_root.drawFocus(argBoolean);
	};

	Videodisplay.getFocusEnabled = function() {
		return b_Videodisplay_root.getFocusEnabled();
	};

	Videodisplay.setFocusEnabled = function(argBoolean) {
		b_Videodisplay_root.setFocusEnabled(argBoolean);
	};

	Videodisplay.removeEventListener = function(argString, argFunction, argBoolean) {
		b_Videodisplay_root.removeEventListener(argString, argFunction, argBoolean);
	};

	Videodisplay.createComponentsFromDescriptors = function(argBoolean) {
		b_Videodisplay_root.createComponentsFromDescriptors(argBoolean);
	};

	Videodisplay.getViewMetricsAndPadding = function() {
		return b_Videodisplay_root.getViewMetricsAndPadding();
	};

	Videodisplay.getMaxVerticalScrollPosition = function() {
		return b_Videodisplay_root.getMaxVerticalScrollPosition();
	};

	Videodisplay.getVerticalLineScrollSize = function() {
		return b_Videodisplay_root.getVerticalLineScrollSize();
	};

	Videodisplay.setVerticalLineScrollSize = function(argNumber) {
		b_Videodisplay_root.setVerticalLineScrollSize(argNumber);
	};

	Videodisplay.getIcon = function() {
		return b_Videodisplay_root.getIcon();
	};

	Videodisplay.setIcon = function(argClass) {
		b_Videodisplay_root.setIcon(argClass);
	};

	Videodisplay.regenerateStyleCache = function(argBoolean) {
		b_Videodisplay_root.regenerateStyleCache(argBoolean);
	};

	Videodisplay.localToContent = function(argPoint) {
		return b_Videodisplay_root.localToContent(argPoint);
	};

	Videodisplay.styleChanged = function(argString) {
		b_Videodisplay_root.styleChanged(argString);
	};

	Videodisplay.notifyStyleChangeInChildren = function(argString, argBoolean) {
		b_Videodisplay_root.notifyStyleChangeInChildren(argString, argBoolean);
	};

	Videodisplay.getHorizontalScrollPosition = function() {
		return b_Videodisplay_root.getHorizontalScrollPosition();
	};

	Videodisplay.setHorizontalScrollPosition = function(argNumber) {
		b_Videodisplay_root.setHorizontalScrollPosition(argNumber);
	};

	Videodisplay.getNumChildren = function() {
		return b_Videodisplay_root.getNumChildren();
	};

	Videodisplay.getLabel = function() {
		return b_Videodisplay_root.getLabel();
	};

	Videodisplay.setLabel = function(argString) {
		b_Videodisplay_root.setLabel(argString);
	};

	Videodisplay.getCreatingContentPane = function() {
		return b_Videodisplay_root.getCreatingContentPane();
	};

	Videodisplay.setCreatingContentPane = function(argBoolean) {
		b_Videodisplay_root.setCreatingContentPane(argBoolean);
	};

	Videodisplay.getHorizontalScrollPolicy = function() {
		return b_Videodisplay_root.getHorizontalScrollPolicy();
	};

	Videodisplay.setHorizontalScrollPolicy = function(argString) {
		b_Videodisplay_root.setHorizontalScrollPolicy(argString);
	};

	Videodisplay.contains = function(argDisplayObject) {
		return b_Videodisplay_root.contains(argDisplayObject);
	};

	Videodisplay.getHorizontalPageScrollSize = function() {
		return b_Videodisplay_root.getHorizontalPageScrollSize();
	};

	Videodisplay.setHorizontalPageScrollSize = function(argNumber) {
		b_Videodisplay_root.setHorizontalPageScrollSize(argNumber);
	};

	Videodisplay.globalToContent = function(argPoint) {
		return b_Videodisplay_root.globalToContent(argPoint);
	};

	Videodisplay.getBorderMetrics = function() {
		return b_Videodisplay_root.getBorderMetrics();
	};

	Videodisplay.removeChild = function(argDisplayObject) {
		return b_Videodisplay_root.removeChild(argDisplayObject);
	};

	Videodisplay.getAutoLayout = function() {
		return b_Videodisplay_root.getAutoLayout();
	};

	Videodisplay.setAutoLayout = function(argBoolean) {
		b_Videodisplay_root.setAutoLayout(argBoolean);
	};

	Videodisplay.addEventListener = function(argString, argFunction, argBoolean1, argInt, argBoolean2) {
		b_Videodisplay_root.addEventListener(argString, argFunction, argBoolean1, argInt, argBoolean2);
	};

	Videodisplay.setChildIndex = function(argDisplayObject, argInt) {
		b_Videodisplay_root.setChildIndex(argDisplayObject, argInt);
	};

	Videodisplay.getChildren = function() {
		return b_Videodisplay_root.getChildren();
	};

	Videodisplay.setDoubleClickEnabled = function(argBoolean) {
		b_Videodisplay_root.setDoubleClickEnabled(argBoolean);
	};

	Videodisplay.getChildByName = function(argString) {
		return b_Videodisplay_root.getChildByName(argString);
	};

	Videodisplay.getVerticalScrollPolicy = function() {
		return b_Videodisplay_root.getVerticalScrollPolicy();
	};

	Videodisplay.setVerticalScrollPolicy = function(argString) {
		b_Videodisplay_root.setVerticalScrollPolicy(argString);
	};

	Videodisplay.finishPrint = function(argObject, argIFlexDisplayObject) {
		b_Videodisplay_root.finishPrint(argObject, argIFlexDisplayObject);
	};

	Videodisplay.getVerticalScrollPosition = function() {
		return b_Videodisplay_root.getVerticalScrollPosition();
	};

	Videodisplay.setVerticalScrollPosition = function(argNumber) {
		b_Videodisplay_root.setVerticalScrollPosition(argNumber);
	};

	Videodisplay.getCreationPolicy = function() {
		return b_Videodisplay_root.getCreationPolicy();
	};

	Videodisplay.setCreationPolicy = function(argString) {
		b_Videodisplay_root.setCreationPolicy(argString);
	};

	Videodisplay.setEnabled = function(argBoolean) {
		b_Videodisplay_root.setEnabled(argBoolean);
	};

	Videodisplay.getContentMouseY = function() {
		return b_Videodisplay_root.getContentMouseY();
	};

	Videodisplay.getContentMouseX = function() {
		return b_Videodisplay_root.getContentMouseX();
	};

	Videodisplay.contentToLocal = function(argPoint) {
		return b_Videodisplay_root.contentToLocal(argPoint);
	};

	Videodisplay.validateDisplayList = function() {
		b_Videodisplay_root.validateDisplayList();
	};

	Videodisplay.getVerticalPageScrollSize = function() {
		return b_Videodisplay_root.getVerticalPageScrollSize();
	};

	Videodisplay.setVerticalPageScrollSize = function(argNumber) {
		b_Videodisplay_root.setVerticalPageScrollSize(argNumber);
	};

	Videodisplay.Container = function() {
		return b_Videodisplay_root.Container();
	};

	Videodisplay.getBaselinePosition = function() {
		return b_Videodisplay_root.getBaselinePosition();
	};

	Videodisplay.getChildDescriptors = function() {
		return b_Videodisplay_root.getChildDescriptors();
	};

	Videodisplay.getData = function() {
		return b_Videodisplay_root.getData();
	};

	Videodisplay.setData = function(argObject) {
		b_Videodisplay_root.setData(argObject);
	};

	Videodisplay.getChildAt = function(argInt) {
		return b_Videodisplay_root.getChildAt(argInt);
	};

	Videodisplay.removeChildAt = function(argInt) {
		return b_Videodisplay_root.removeChildAt(argInt);
	};

	Videodisplay.contentToGlobal = function(argPoint) {
		return b_Videodisplay_root.contentToGlobal(argPoint);
	};

	Videodisplay.getChildIndex = function(argDisplayObject) {
		return b_Videodisplay_root.getChildIndex(argDisplayObject);
	};

	Videodisplay.initialize = function() {
		b_Videodisplay_root.initialize();
	};

	Videodisplay.getMaxHorizontalScrollPosition = function() {
		return b_Videodisplay_root.getMaxHorizontalScrollPosition();
	};

	Videodisplay.getViewMetrics = function() {
		return b_Videodisplay_root.getViewMetrics();
	};

	Videodisplay.getRawChildren = function() {
		return b_Videodisplay_root.getRawChildren();
	};

	Videodisplay.executeChildBindings = function(argBoolean) {
		b_Videodisplay_root.executeChildBindings(argBoolean);
	};

	Videodisplay.getHorizontalLineScrollSize = function() {
		return b_Videodisplay_root.getHorizontalLineScrollSize();
	};

	Videodisplay.setHorizontalLineScrollSize = function(argNumber) {
		b_Videodisplay_root.setHorizontalLineScrollSize(argNumber);
	};

	Videodisplay.getClipContent = function() {
		return b_Videodisplay_root.getClipContent();
	};

	Videodisplay.setClipContent = function(argBoolean) {
		b_Videodisplay_root.setClipContent(argBoolean);
	};

	Videodisplay.createComponentFromDescriptor = function(argComponentDescriptor, argBoolean) {
		return b_Videodisplay_root.createComponentFromDescriptor(argComponentDescriptor, argBoolean);
	};

	Videodisplay.getDefaultButton = function() {
		return b_Videodisplay_root.getDefaultButton();
	};

	Videodisplay.setDefaultButton = function(argIFlexDisplayObject) {
		b_Videodisplay_root.setDefaultButton(argIFlexDisplayObject);
	};

	Videodisplay.executeBindings = function(argBoolean) {
		b_Videodisplay_root.executeBindings(argBoolean);
	};

	Videodisplay.getVerticalScrollBar = function() {
		return b_Videodisplay_root.getVerticalScrollBar();
	};

	Videodisplay.setVerticalScrollBar = function(argScrollBar) {
		b_Videodisplay_root.setVerticalScrollBar(argScrollBar);
	};

	Videodisplay.addChild = function(argDisplayObject) {
		return b_Videodisplay_root.addChild(argDisplayObject);
	};

	Videodisplay.getHorizontalScrollBar = function() {
		return b_Videodisplay_root.getHorizontalScrollBar();
	};

	Videodisplay.setHorizontalScrollBar = function(argScrollBar) {
		b_Videodisplay_root.setHorizontalScrollBar(argScrollBar);
	};

	Videodisplay.addChildAt = function(argDisplayObject, argInt) {
		return b_Videodisplay_root.addChildAt(argDisplayObject, argInt);
	};

	Videodisplay.getCreationIndex = function() {
		return b_Videodisplay_root.getCreationIndex();
	};

	Videodisplay.setCreationIndex = function(argInt) {
		b_Videodisplay_root.setCreationIndex(argInt);
	};

	Videodisplay.getFocusPane = function() {
		return b_Videodisplay_root.getFocusPane();
	};

	Videodisplay.setFocusPane = function(argSprite) {
		b_Videodisplay_root.setFocusPane(argSprite);
	};

	Videodisplay.validateSize = function(argBoolean) {
		b_Videodisplay_root.validateSize(argBoolean);
	};

	Videodisplay.removeAllChildren = function() {
		b_Videodisplay_root.removeAllChildren();
	};

	Videodisplay.prepareToPrint = function(argIFlexDisplayObject) {
		return b_Videodisplay_root.prepareToPrint(argIFlexDisplayObject);
	};

	Videodisplay.FlexSprite = function() {
		return b_Videodisplay_root.FlexSprite();
	};

	Videodisplay.toString = function() {
		return b_Videodisplay_root.toString();
	};

	Videodisplay.getChildIndex = function(argDisplayObject) {
		return b_Videodisplay_root.getChildIndex(argDisplayObject);
	};

	Videodisplay.getChildByName = function(argString) {
		return b_Videodisplay_root.getChildByName(argString);
	};

	Videodisplay.getNumChildren = function() {
		return b_Videodisplay_root.getNumChildren();
	};

	Videodisplay.setChildIndex = function(argDisplayObject, argInt) {
		b_Videodisplay_root.setChildIndex(argDisplayObject, argInt);
	};

	Videodisplay.getTabChildren = function() {
		return b_Videodisplay_root.getTabChildren();
	};

	Videodisplay.setTabChildren = function(argBoolean) {
		b_Videodisplay_root.setTabChildren(argBoolean);
	};

	Videodisplay.addChild = function(argDisplayObject) {
		return b_Videodisplay_root.addChild(argDisplayObject);
	};

	Videodisplay.swapChildren = function(argDisplayObject1, argDisplayObject2) {
		b_Videodisplay_root.swapChildren(argDisplayObject1, argDisplayObject2);
	};

	Videodisplay.removeChild = function(argDisplayObject) {
		return b_Videodisplay_root.removeChild(argDisplayObject);
	};

	Videodisplay.contains = function(argDisplayObject) {
		return b_Videodisplay_root.contains(argDisplayObject);
	};

	Videodisplay.removeChildAt = function(argInt) {
		return b_Videodisplay_root.removeChildAt(argInt);
	};

	Videodisplay.getTextSnapshot = function() {
		return b_Videodisplay_root.getTextSnapshot();
	};

	Videodisplay.swapChildrenAt = function(argInt1, argInt2) {
		b_Videodisplay_root.swapChildrenAt(argInt1, argInt2);
	};

	Videodisplay.getMouseChildren = function() {
		return b_Videodisplay_root.getMouseChildren();
	};

	Videodisplay.setMouseChildren = function(argBoolean) {
		b_Videodisplay_root.setMouseChildren(argBoolean);
	};

	Videodisplay.areInaccessibleObjectsUnderPoint = function(argPoint) {
		return b_Videodisplay_root.areInaccessibleObjectsUnderPoint(argPoint);
	};

	Videodisplay.DisplayObjectContainer = function() {
		return b_Videodisplay_root.DisplayObjectContainer();
	};

	Videodisplay.getChildAt = function(argInt) {
		return b_Videodisplay_root.getChildAt(argInt);
	};

	Videodisplay.getObjectsUnderPoint = function(argPoint) {
		return b_Videodisplay_root.getObjectsUnderPoint(argPoint);
	};

	Videodisplay.addChildAt = function(argDisplayObject, argInt) {
		return b_Videodisplay_root.addChildAt(argDisplayObject, argInt);
	};

	Videodisplay.LayoutContainer = function() {
		return b_Videodisplay_root.LayoutContainer();
	};

	Videodisplay.getConstraintColumns = function() {
		return b_Videodisplay_root.getConstraintColumns();
	};

	Videodisplay.setConstraintColumns = function(argArray) {
		b_Videodisplay_root.setConstraintColumns(argArray);
	};

	Videodisplay.getLayout = function() {
		return b_Videodisplay_root.getLayout();
	};

	Videodisplay.setLayout = function(argString) {
		b_Videodisplay_root.setLayout(argString);
	};

	Videodisplay.getConstraintRows = function() {
		return b_Videodisplay_root.getConstraintRows();
	};

	Videodisplay.setConstraintRows = function(argArray) {
		b_Videodisplay_root.setConstraintRows(argArray);
	};

	Videodisplay.getWidth = function() {
		return b_Videodisplay_root.getWidth();
	};

	Videodisplay.setWidth = function(argNumber) {
		b_Videodisplay_root.setWidth(argNumber);
	};

	Videodisplay.getHeight = function() {
		return b_Videodisplay_root.getHeight();
	};

	Videodisplay.setHeight = function(argNumber) {
		b_Videodisplay_root.setHeight(argNumber);
	};

	Videodisplay.getRect = function(argDisplayObject) {
		return b_Videodisplay_root.getRect(argDisplayObject);
	};

	Videodisplay.getScale9Grid = function() {
		return b_Videodisplay_root.getScale9Grid();
	};

	Videodisplay.setScale9Grid = function(argRectangle) {
		b_Videodisplay_root.setScale9Grid(argRectangle);
	};

	Videodisplay.hitTestObject = function(argDisplayObject) {
		return b_Videodisplay_root.hitTestObject(argDisplayObject);
	};

	Videodisplay.getBounds = function(argDisplayObject) {
		return b_Videodisplay_root.getBounds(argDisplayObject);
	};

	Videodisplay.hitTestPoint = function(argNumber1, argNumber2, argBoolean) {
		return b_Videodisplay_root.hitTestPoint(argNumber1, argNumber2, argBoolean);
	};

	Videodisplay.getStage = function() {
		return b_Videodisplay_root.getStage();
	};

	Videodisplay.getParent = function() {
		return b_Videodisplay_root.getParent();
	};

	Videodisplay.localToGlobal = function(argPoint) {
		return b_Videodisplay_root.localToGlobal(argPoint);
	};

	Videodisplay.getLoaderInfo = function() {
		return b_Videodisplay_root.getLoaderInfo();
	};

	Videodisplay.getRotationZ = function() {
		return b_Videodisplay_root.getRotationZ();
	};

	Videodisplay.setRotationZ = function(argNumber) {
		b_Videodisplay_root.setRotationZ(argNumber);
	};

	Videodisplay.getRotationY = function() {
		return b_Videodisplay_root.getRotationY();
	};

	Videodisplay.setRotationY = function(argNumber) {
		b_Videodisplay_root.setRotationY(argNumber);
	};

	Videodisplay.getName = function() {
		return b_Videodisplay_root.getName();
	};

	Videodisplay.setName = function(argString) {
		b_Videodisplay_root.setName(argString);
	};

	Videodisplay.getRotationX = function() {
		return b_Videodisplay_root.getRotationX();
	};

	Videodisplay.setRotationX = function(argNumber) {
		b_Videodisplay_root.setRotationX(argNumber);
	};

	Videodisplay.getOpaqueBackground = function() {
		return b_Videodisplay_root.getOpaqueBackground();
	};

	Videodisplay.setOpaqueBackground = function(argObject) {
		b_Videodisplay_root.setOpaqueBackground(argObject);
	};

	Videodisplay.getCacheAsBitmap = function() {
		return b_Videodisplay_root.getCacheAsBitmap();
	};

	Videodisplay.setCacheAsBitmap = function(argBoolean) {
		b_Videodisplay_root.setCacheAsBitmap(argBoolean);
	};

	Videodisplay.getFilters = function() {
		return b_Videodisplay_root.getFilters();
	};

	Videodisplay.setFilters = function(argArray) {
		b_Videodisplay_root.setFilters(argArray);
	};

	Videodisplay.getAccessibilityProperties = function() {
		return b_Videodisplay_root.getAccessibilityProperties();
	};

	Videodisplay.setAccessibilityProperties = function(argAccessibilityProperties) {
		b_Videodisplay_root.setAccessibilityProperties(argAccessibilityProperties);
	};

	Videodisplay.getVisible = function() {
		return b_Videodisplay_root.getVisible();
	};

	Videodisplay.setVisible = function(argBoolean) {
		b_Videodisplay_root.setVisible(argBoolean);
	};

	Videodisplay.getRoot = function() {
		return b_Videodisplay_root.getRoot();
	};

	Videodisplay.setBlendShader = function(argShader) {
		b_Videodisplay_root.setBlendShader(argShader);
	};

	Videodisplay.getTransform = function() {
		return b_Videodisplay_root.getTransform();
	};

	Videodisplay.setTransform = function(argTransform) {
		b_Videodisplay_root.setTransform(argTransform);
	};

	Videodisplay.getRotation = function() {
		return b_Videodisplay_root.getRotation();
	};

	Videodisplay.setRotation = function(argNumber) {
		b_Videodisplay_root.setRotation(argNumber);
	};

	Videodisplay.getScaleZ = function() {
		return b_Videodisplay_root.getScaleZ();
	};

	Videodisplay.setScaleZ = function(argNumber) {
		b_Videodisplay_root.setScaleZ(argNumber);
	};

	Videodisplay.getScaleY = function() {
		return b_Videodisplay_root.getScaleY();
	};

	Videodisplay.setScaleY = function(argNumber) {
		b_Videodisplay_root.setScaleY(argNumber);
	};

	Videodisplay.getScaleX = function() {
		return b_Videodisplay_root.getScaleX();
	};

	Videodisplay.setScaleX = function(argNumber) {
		b_Videodisplay_root.setScaleX(argNumber);
	};

	Videodisplay.getMouseY = function() {
		return b_Videodisplay_root.getMouseY();
	};

	Videodisplay.getMouseX = function() {
		return b_Videodisplay_root.getMouseX();
	};

	Videodisplay.getZ = function() {
		return b_Videodisplay_root.getZ();
	};

	Videodisplay.setZ = function(argNumber) {
		b_Videodisplay_root.setZ(argNumber);
	};

	Videodisplay.getY = function() {
		return b_Videodisplay_root.getY();
	};

	Videodisplay.setY = function(argNumber) {
		b_Videodisplay_root.setY(argNumber);
	};

	Videodisplay.getX = function() {
		return b_Videodisplay_root.getX();
	};

	Videodisplay.setX = function(argNumber) {
		b_Videodisplay_root.setX(argNumber);
	};

	Videodisplay.local3DToGlobal = function(argVector3D) {
		return b_Videodisplay_root.local3DToGlobal(argVector3D);
	};

	Videodisplay.getMask = function() {
		return b_Videodisplay_root.getMask();
	};

	Videodisplay.setMask = function(argDisplayObject) {
		b_Videodisplay_root.setMask(argDisplayObject);
	};

	Videodisplay.DisplayObject = function() {
		return b_Videodisplay_root.DisplayObject();
	};

	Videodisplay.getAlpha = function() {
		return b_Videodisplay_root.getAlpha();
	};

	Videodisplay.setAlpha = function(argNumber) {
		b_Videodisplay_root.setAlpha(argNumber);
	};

	Videodisplay.getScrollRect = function() {
		return b_Videodisplay_root.getScrollRect();
	};

	Videodisplay.setScrollRect = function(argRectangle) {
		b_Videodisplay_root.setScrollRect(argRectangle);
	};

	Videodisplay.getBlendMode = function() {
		return b_Videodisplay_root.getBlendMode();
	};

	Videodisplay.setBlendMode = function(argString) {
		b_Videodisplay_root.setBlendMode(argString);
	};

	Videodisplay.globalToLocal3D = function(argPoint) {
		return b_Videodisplay_root.globalToLocal3D(argPoint);
	};

	Videodisplay.globalToLocal = function(argPoint) {
		return b_Videodisplay_root.globalToLocal(argPoint);
	};

	Videodisplay.finishPrint = function(argObject, argIFlexDisplayObject) {
		b_Videodisplay_root.finishPrint(argObject, argIFlexDisplayObject);
	};

	Videodisplay.setPercentWidth = function(argNumber) {
		b_Videodisplay_root.setPercentWidth(argNumber);
	};

	Videodisplay.getUrl = function() {
		return b_Videodisplay_root.getUrl();
	};

	Videodisplay.initialize = function() {
		b_Videodisplay_root.initialize();
	};

	Videodisplay.styleChanged = function(argString) {
		b_Videodisplay_root.styleChanged(argString);
	};

	Videodisplay.getViewSourceURL = function() {
		return b_Videodisplay_root.getViewSourceURL();
	};

	Videodisplay.setViewSourceURL = function(argString) {
		b_Videodisplay_root.setViewSourceURL(argString);
	};

	Videodisplay.setIcon = function(argClass) {
		b_Videodisplay_root.setIcon(argClass);
	};

	Videodisplay.setTabIndex = function(argInt) {
		b_Videodisplay_root.setTabIndex(argInt);
	};

	Videodisplay.setToolTip = function(argString) {
		b_Videodisplay_root.setToolTip(argString);
	};

	Videodisplay.getViewMetrics = function() {
		return b_Videodisplay_root.getViewMetrics();
	};

	Videodisplay.prepareToPrint = function(argIFlexDisplayObject) {
		return b_Videodisplay_root.prepareToPrint(argIFlexDisplayObject);
	};

	Videodisplay.getId = function() {
		return b_Videodisplay_root.getId();
	};

	Videodisplay.Application = function() {
		return b_Videodisplay_root.Application();
	};

	Videodisplay.setPercentHeight = function(argNumber) {
		b_Videodisplay_root.setPercentHeight(argNumber);
	};

	Videodisplay.setLabel = function(argString) {
		b_Videodisplay_root.setLabel(argString);
	};

	Videodisplay.getParameters = function() {
		return b_Videodisplay_root.getParameters();
	};

	Videodisplay.getChildIndex = function(argDisplayObject) {
		return b_Videodisplay_root.getChildIndex(argDisplayObject);
	};

	Videodisplay.addToCreationQueue = function(argObject, argInt, argFunction, argIFlexDisplayObject) {
		b_Videodisplay_root.addToCreationQueue(argObject, argInt, argFunction, argIFlexDisplayObject);
	};

	Videodisplay.toString = function() {
		return b_Videodisplay_root.toString();
	};

	Videodisplay.hasOwnProperty = function(argString) {
		return b_Videodisplay_root.hasOwnProperty(argString);
	};

	Videodisplay.isPrototypeOf = function(argObject) {
		return b_Videodisplay_root.isPrototypeOf(argObject);
	};

	Videodisplay.propertyIsEnumerable = function(argString) {
		return b_Videodisplay_root.propertyIsEnumerable(argString);
	};

	Videodisplay.Object = function() {
		return b_Videodisplay_root.Object();
	};

	Videodisplay.toLocaleString = function() {
		return b_Videodisplay_root.toLocaleString();
	};

	Videodisplay.setPropertyIsEnumerable = function(argString, argBoolean) {
		b_Videodisplay_root.setPropertyIsEnumerable(argString, argBoolean);
	};

	Videodisplay.valueOf = function() {
		return b_Videodisplay_root.valueOf();
	};

	Videodisplay.willTrigger = function(argString) {
		return b_Videodisplay_root.willTrigger(argString);
	};

	Videodisplay.toString = function() {
		return b_Videodisplay_root.toString();
	};

	Videodisplay.removeEventListener = function(argString, argFunction, argBoolean) {
		b_Videodisplay_root.removeEventListener(argString, argFunction, argBoolean);
	};

	Videodisplay.EventDispatcher = function(argIEventDispatcher) {
		return b_Videodisplay_root.EventDispatcher(argIEventDispatcher);
	};

	Videodisplay.addEventListener = function(argString, argFunction, argBoolean1, argInt, argBoolean2) {
		b_Videodisplay_root.addEventListener(argString, argFunction, argBoolean1, argInt, argBoolean2);
	};

	Videodisplay.hasEventListener = function(argString) {
		return b_Videodisplay_root.hasEventListener(argString);
	};

	Videodisplay.dispatchEvent = function(argEvent) {
		return b_Videodisplay_root.dispatchEvent(argEvent);
	};

	Videodisplay.getTabIndex = function() {
		return b_Videodisplay_root.getTabIndex();
	};

	Videodisplay.setTabIndex = function(argInt) {
		b_Videodisplay_root.setTabIndex(argInt);
	};

	Videodisplay.InteractiveObject = function() {
		return b_Videodisplay_root.InteractiveObject();
	};

	Videodisplay.getTabEnabled = function() {
		return b_Videodisplay_root.getTabEnabled();
	};

	Videodisplay.setTabEnabled = function(argBoolean) {
		b_Videodisplay_root.setTabEnabled(argBoolean);
	};

	Videodisplay.getAccessibilityImplementation = function() {
		return b_Videodisplay_root.getAccessibilityImplementation();
	};

	Videodisplay.setAccessibilityImplementation = function(argAccessibilityImplementation) {
		b_Videodisplay_root.setAccessibilityImplementation(argAccessibilityImplementation);
	};

	Videodisplay.getMouseEnabled = function() {
		return b_Videodisplay_root.getMouseEnabled();
	};

	Videodisplay.setMouseEnabled = function(argBoolean) {
		b_Videodisplay_root.setMouseEnabled(argBoolean);
	};

	Videodisplay.getContextMenu = function() {
		return b_Videodisplay_root.getContextMenu();
	};

	Videodisplay.setContextMenu = function(argContextMenu) {
		b_Videodisplay_root.setContextMenu(argContextMenu);
	};

	Videodisplay.getDoubleClickEnabled = function() {
		return b_Videodisplay_root.getDoubleClickEnabled();
	};

	Videodisplay.setDoubleClickEnabled = function(argBoolean) {
		b_Videodisplay_root.setDoubleClickEnabled(argBoolean);
	};

	Videodisplay.getFocusRect = function() {
		return b_Videodisplay_root.getFocusRect();
	};

	Videodisplay.setFocusRect = function(argObject) {
		b_Videodisplay_root.setFocusRect(argObject);
	};

	Videodisplay.getHitArea = function() {
		return b_Videodisplay_root.getHitArea();
	};

	Videodisplay.setHitArea = function(argSprite) {
		b_Videodisplay_root.setHitArea(argSprite);
	};

	Videodisplay.getDropTarget = function() {
		return b_Videodisplay_root.getDropTarget();
	};

	Videodisplay.Sprite = function() {
		return b_Videodisplay_root.Sprite();
	};

	Videodisplay.getUseHandCursor = function() {
		return b_Videodisplay_root.getUseHandCursor();
	};

	Videodisplay.setUseHandCursor = function(argBoolean) {
		b_Videodisplay_root.setUseHandCursor(argBoolean);
	};

	Videodisplay.stopDrag = function() {
		b_Videodisplay_root.stopDrag();
	};

	Videodisplay.startDrag = function(argBoolean, argRectangle) {
		b_Videodisplay_root.startDrag(argBoolean, argRectangle);
	};

	Videodisplay.getButtonMode = function() {
		return b_Videodisplay_root.getButtonMode();
	};

	Videodisplay.setButtonMode = function(argBoolean) {
		b_Videodisplay_root.setButtonMode(argBoolean);
	};

	Videodisplay.getSoundTransform = function() {
		return b_Videodisplay_root.getSoundTransform();
	};

	Videodisplay.setSoundTransform = function(argSoundTransform) {
		b_Videodisplay_root.setSoundTransform(argSoundTransform);
	};

	Videodisplay.getGraphics = function() {
		return b_Videodisplay_root.getGraphics();
	};

}
