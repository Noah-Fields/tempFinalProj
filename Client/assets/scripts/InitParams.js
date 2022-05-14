var spinSpeed = 0.005;

var radius = 0.5;

var winWidth = 1920;
var winHeight = 1080;

var xGrav = 0.0;
var yGrav = -10.0;
var zGrav = 0.0;
var running = true;

var initAvX = -1.0;
var initAvY = 1.4;
var initAvZ = 1.0;
var initAvScaleX = 0.6;
var initAvScaleY = 0.6;
var initAvScaleZ = 0.6;

var initGroundX = 0.0;
var initGroundY = 0.0;
var initGroundZ = 0.0;
var initGroundScaleX = 150.0;
var initGroundScaleY = 15.0;
var initGroundScaleZ = 150.0;

var health = 5;
var avMass = 2.0;
var groundBounciness = 1.0;
var ballBounciness = 1.0;

// Because most objects are created upon throwing/taking an action or are server side,
// there are not many objects which need to have their location initialized