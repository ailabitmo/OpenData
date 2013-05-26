/**
 * iBrowser = "interactive Browser"
 * 
 * Dynamic graph rendering for RDF-like data.
 * 
 * This lib uses the RaphaelJS abstraction for SVG and VML.
 * 
 * @author Uli
 */

/**
 * Returns the 4 connectable points for the given boundary box.
 */
function getConnectablePoints( box )
{
	// Given a box, return the 4 connectable points
	// i.e. the middle of each side of the rectangle.

	// Which placeholder shall we use (distance to the boundary box)
	var rd = 0;

	// Return the array with the 4 connectable points on the
	// boundary box
	return [ { x : box.x + box.width / 2, y : box.y - rd },
	          { x : box.x + box.width / 2, y : box.y + box.height + rd },
	          { x : box.x - rd, y : box.y + box.height / 2 },
	          { x : box.x + box.width + rd, y : box.y + box.height / 2 } ];	
}

/**
 * Returns a Bezier path that connects object 1 with object 2.
 * 
 * @param obj1 First object
 * @param obj2 Second object
 * @param attributes Attributes to use for the path
 */
Raphael.fn.connect = function(obj1, obj2, attributes)
{
	// Calculate the boundary boxes of the objects 
	var box1 = obj1.getBBox();
	var box2 = obj2.getBBox();
	
	// Calculate the "connectable points"
	var p = getConnectablePoints( box1 );
	var q = getConnectablePoints( box2 );

	// Find the two nearest points that are connectable
	// This basically finds the minimum of (dx[i] + dy[j])
	var res = [0,0];
	var diff = 1e7;
	for ( var i = 0; i < p.length; i++)
	{
		for ( var j = 0; j < q.length; j++)
		{
			var dx = Math.abs(p[i].x - q[j].x), dy = Math.abs(p[i].y - q[j].y);
			
			if ( dx+dy < diff )
			{
				diff = dx+dy;
				res = [i,j];
			}
		}
	}
	
	var x1 = p[res[0]].x, 
		y1 = p[res[0]].y,
		x4 = q[res[1]].x,
		y4 = q[res[1]].y,
		dx = Math.max(Math.abs(x1 - x4) / 2, 10),
		dy = Math.max(Math.abs(y1 - y4) / 2, 10);
		
	// Here we calculate the 4 different types of Bezier points,
	// and select the ones according to the points we want
	// to connect to.
	var x2 = [ x1, x1, x1 - dx, x1 + dx ][res[0]].toFixed(3),
		y2 = [ y1 - dy, y1 + dy, y1, y1 ][res[0]].toFixed(3),
		x3 = [ x4, x4, x4 - dx, x4 + dx ][res[1]].toFixed(3),
		y3 = [ y1 + dy, y1 - dy, y4, y4 ][res[1]].toFixed(3);

	// Assemble the SVG/VML path that connects the objects
	var pathDef = [ "M", x1.toFixed(3), y1.toFixed(3),
	             "C", x2, y2, x3, y3,
	             x4.toFixed(3), y4.toFixed(3),
	           ].join(",");

	// Return the objectified result
	return {
		bg : this.path(pathDef).attr( attributes )
	};
}

/**
 * Renders the graph for the given entity,
 * with given incoming and outgoing edges.
 * 
 * @param holderId ID of the holding <div>
 * @param holderX Width
 * @param holderY Height
 * @param entityTextIn Textual name of the entity
 * @param incomingIn Array of incoming edges
 * @param outgoingIn Array of outgoing edges
 * @return Renders the graph in the given holder element.
 */
function drawGraph( holderId, holderX, holderY, entityTextIn, incomingIn, outgoingIn )
{
	var r = Raphael( holderId, holderX, holderY);
	var connections = [], shapes = [];
	
	// Defined attributes for the different graph elements
	// TODO: make configurable
	var attEntity = {"font-size":"12", "font-weight":"bold"};
	var attEntityBox = { stroke:"#008", "stroke-width":"2" };
	var attTextLeft = {"text-anchor":"end", "font-size":"12", "font-weight":"bold"};
	var attLeftDot = {fill:"#800",stroke:"#800", "stroke-width":"2"};
	var attRightDot = attLeftDot;
	var attTextRight = {"text-anchor":"start", "font-size":"12", "font-weight":"bold"};
	var attLine = {stroke:"#888", "stroke-width":"2"};
	
	// Render the graph elements
	
	// The central entity
	var entityText = r.text(320+70,180+20,entityTextIn);
	entityText.attr( attEntity );
	
	var eb = entityText.getBBox();
	
	var sp = 10;
	var entity = r.rect( eb.x-sp, eb.y-sp, eb.width+2*sp, eb.height+2*sp, 10);
	entity.attr( attEntityBox );
	
	shapes.push(entity);
	var o;
	
	// Loop over the incoming edges
	for ( var i = 0; i < incomingIn.length; i++)
	{
		var x = 150;
		var y = 20 + 40*i;
		var dir = 1;
		
		var text = incomingIn[i].text;
		var rel = incomingIn[i].rel;
		var href = incomingIn[i].href;
		var target = incomingIn[i].target;
		
		o = r.text(x - dir*10, y, text).attr( attTextLeft );
		if (href) o.attr( {'href':href} );
		if (target) o.attr( {'target':target} );
		if (href)
		{
		o = r.rect(x - dir*5, y - 5, 10, 10, 10).attr( attLeftDot );
		if (href) o.attr( {'href':href} );
		if (target) o.attr( {'target':target} );
		}
		
		r.text(x + dir*30, y -10, rel);

		var line = r.path("M,"+(x+dir*5)+","+y+",l,"+(dir*50)+",0");
		line.attr( attLine );
		shapes.push( line  );
	}

	// Loop over outgoing edges
	for ( var i = 0; i < outgoingIn.length; i++)
	{
		var x = 650;
		var y = 20 + 40*i;
		var dir = -1;
		
		var text = outgoingIn[i].text;
		var rel = outgoingIn[i].rel;		
		var href = outgoingIn[i].href;
		var target = outgoingIn[i].target;
		
		o = r.text(x - dir*15, y, text).attr( attTextRight );
		if (href) o.attr( {'href':href} );
		if (target) o.attr( {'target':target} );
		if (href)
		{
		o = r.rect(x, y - 5, 10, 10, 10).attr( attRightDot );
		if (href) o.attr( {'href':href} );
		if (target) o.attr( {'target':target} );
		}
		r.text(x + dir*30, y -10, rel);

		var line = r.path("M,"+(x)+","+y+",l,"+(dir*50)+",0");
		line.attr( attLine );
		shapes.push( line  );
	}

	// Render Bezier connections
	for ( var i = 1; i < shapes.length; i++)
	{
		connections.push(r.connect(shapes[0], shapes[i], attLine ));
	}

	// Correct Z-order visibility
	entity.toFront();
	entityText.toFront();
};
