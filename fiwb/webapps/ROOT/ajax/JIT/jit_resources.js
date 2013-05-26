var labelType, useGradients, nativeTextSupport, animate;

(function() {
  var ua = navigator.userAgent,
      iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i),
      typeOfCanvas = typeof HTMLCanvasElement,
      nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'),
      textSupport = nativeCanvasSupport 
        && (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
  //I'm setting this based on the fact that ExCanvas provides text support for IE
  //and that as of today iPhone/iPad current text support is lame
  labelType = (!nativeCanvasSupport || (textSupport && !iStuff))? 'Native' : 'HTML';
  nativeTextSupport = labelType == 'Native';
  useGradients = nativeCanvasSupport;
  animate = !(iStuff || !nativeCanvasSupport);
})();


//print the image to the canvas
function printImage(canvas, img, node, sizex, sizey)
{
    //if the image has no size, try to load it from image store
    if(sizex==0 || sizey==0)
    {
      return
      debugln("invalid image dimensions");
    }

    //position  
    var x = node.data.posX;
    var y = node.data.posY;
    
    if(!x || !y || !img || !img.src)
    {
      debugln("node invalid");
      return;
    }
    debugln(" Pos  "+x+" "+y+" size "+sizex+" "+sizey);

    if(node.data.hidden && node.data.currentState == "hidden")
      debugln("image hidden");
    else
      canvas.getCtx().drawImage(img, x-sizex/2, y-sizey/2, sizex, sizey);
}

//#####################################################################//
//-------------------------------ForceDirectedGraph (pojograph)------------------------------------------------//
//######################################################################//
function initPojoGraph(input1,id1)
{
	var cont = id1;
    //nodes as icons
    $jit.ForceDirected.Plot.NodeTypes.implement({
        'pics': {
              'render': function(node, canvas)
              {
                  
                  if(node.data.img)
                  {
                      debugln("drawing image "+node.data.img);

    
                      if(node.data.hidden)
                      {
                          if(node.data.currentState == "hidden")
                              {
                                node.setData('alpha',0);
                                node.data.alpha='0';
                              }
                          else if(node.data.currentState == "visible")
                          {
                              node.setData('alpha',1);
                              node.data.alpha='1';
                          }
                      }
                      var img = new Image();
                      //dimension
                      img.src = node.data.img;
                      
                      //the image is loaded asynchronously
                      //if not the width/height was only available if it was already int he browser's cache
                      
                      var imgLoaded = img.width !=0 && img.height!=0;
                      if(!imgLoaded)
                      {
                          //asynchronous printing
                          img.onload = function()
                          {
                              var sizex = this.width;
                              var sizey = this.height;
                              printImage(canvas, this, node, sizex, sizey);
                          };
                      }
                      else
                          //synchronous printing
                          printImage(canvas, img, node, img.width, img.height);   
                              
                  }
                  else
                      debugln("no image found");
                  
              },
              'contains': function(node, pos)
              {
                  if(node.data.img)
                  {
                      var npos = node.pos.getc(true);
                      var img = new Image();
                      img.src = node.data.img;
                      var width = img.width;
                      var height = img.height;
                     
                      return Math.abs(pos.x - npos.x) <= width / 2
                        && Math.abs(pos.y - npos.y) <= height / 2;
                  }
                  else
                      return false;

              }
        }
  });   
  //label placement on edges
    $jit.ForceDirected.Plot.EdgeTypes.implement(
      {
          'labeled':
          {
            'render': function(adj, canvas)
            {
                if(adj.data.hover)
                    ;
                else
                    adj.setData('lineWidth', 2);
                //plot arrow edge
                this.edgeTypes.line.render.call(this, adj, canvas); 
          }
        }
      }
    );
    // init ForceDirected
    var fd = new $jit.ForceDirected({
      //id of the visualization container
      injectInto: cont,
      //set the width according to the width of the wiki text
      width: 810,
      Node: {
        overridable: true,
        dim: 8,
        type: 'pics'
      },
      Edge: {
        overridable: true,
        color: '#000000',
        lineWidth: 0.16,
        type: 'labeled'
      },
      //Add Tips
      Tips: {
        enable: false,
        onShow: function(tip, node) {
          //count connections
          var count = 0;
          node.eachAdjacency(function() { count++; });
          //display node info in tooltip
          tip.innerHTML = "<div class=\"tip-title\">" + node.name + "</div>"
            + "<div class=\"tip-text\"><b>connections:</b> " + count + "</div>";
        }
      },
      // Add node events
      Events: {
        enable: true,
        type: 'Native',
        //Change cursor style when hovering a node
        onClick: function(node, eventInfo, e)
        {
            if(node.data && node.data.link && node.data.link.length > 0)
                document.location = node.data.link;
        },
        
        onMouseEnter: function(node, eventInfo, e)
        {
            fd.canvas.getElement().style.cursor = 'pointer';
            node.eachAdjacency(function(adj)
            {
                if(adj.data.hidden)
                {
                    var nodeToHide;
                    if(adj.nodeFrom.data.hidden)
                    {
                        nodeToHide = adj.nodeFrom;
                    }
                    if(adj.nodeTo.data.hidden)
                    {
                        nodeToHide = adj.nodeTo;
                    }
                    if(nodeToHide)
                    {
                        nodeToHide.setData('currentState','visible');
                        nodeToHide.setData('alpha',1);
                        nodeToHide.data.currentState= 'visible';
                        nodeToHide.data.alpha = '1';
                    }
                }
                
                if(adj.data.hover)
                {
                    adj.setData('lineWidth', 4);
                    adj.setData('color', '#9ed1e7');
                }
            });
            fd.fx.plot();
            
        },
        onMouseLeave: function(node, eventInfo, e)
        {
            fd.canvas.getElement().style.cursor = '';
            node.eachAdjacency(function(adj)
            { 
                if(adj.data.hidden)
                {
                    var nodeToHide;
                    if(adj.nodeFrom.data.hidden)
                    {
                        nodeToHide = adj.nodeFrom;
                    }
                    if(adj.nodeTo.data.hidden)
                    {
                        nodeToHide = adj.nodeTo;
                    }
                    if(nodeToHide)
                    {
                        nodeToHide.setData('currentState','hidden');
                        nodeToHide.setData('alpha',0);
                        nodeToHide.data.currentState= 'hidden';
                        nodeToHide.data.alpha = '0';
                    }
                        
                }
                if(adj.data.hover)
                {
                    adj.setData('lineWidth', 0.16);
                    adj.setData('color', '#000000');
                }
            });
            fd.fx.plot();
        }
      },
      //Number of iterations for the FD algorithm
      iterations: 100,
      //Edge length
      levelDistance: 150,
      // This method is only triggered
      // on label creation and only for DOM labels (not native canvas ones).
      onCreateLabel: function(domElement, node){
        // Create a 'name' and 'close' buttons and add them
        // to the main node label
        var nameContainer = document.createElement('span');
        var style = nameContainer.style;
        nameContainer.className = 'name';
        nameContainer.innerHTML = node.data.labelT;

        domElement.appendChild(nameContainer);
        style.fontSize = "0.8em";
        //Fade the node and its connections when
        //clicking the close button
       
      },
      // Change node styles when DOM labels are placed
      // or moved.
      onPlaceLabel: function(domElement, node){
        var style = domElement.style;
        var left = parseInt(style.left);
        var top = parseInt(style.top);
        var w = domElement.offsetWidth;
        style.left = (left - w / 2) + 'px';
        style.top = (top + 30) + 'px';
//              style.display = '';
      }
    });
    // load JSON data.
    // init data
    var json = input1.array;
    fd.loadJSON(json);
    
    // compute positions incrementally and animate.
    fd.computeIncremental({
        iter: 40,
        property: 'end',
        onComplete: function(){
          debugln('done');
          fd.animate({
            modes: ['linear'],
            transition: $jit.Trans.Elastic.easeOut,
            duration: 200
          });
          //do the positioning only once
          //at the beginning of the initial animation
          fd.graph.eachNode(function(node) {
              var x = node.data.posX;
              var y = node.data.posY;
              
              node.setPos(new $jit.Complex(x, y), 'end');
//              node.pos.setc(x, y);
            });
        }
      });

}

function sleep(milliSeconds){
    var startTime = new Date().getTime(); 
    while (new Date().getTime() < startTime + milliSeconds); 
}
//#####################################################################//
//--------------------------------------------------------Hypertree----------------------------------------------------//
//######################################################################//
function initHypertree(input1, id1){
    //init data
    var cont = id1;
    var json = input1;
    var infovis = document.getElementById(cont);
    if (infovis==null)
    {
       return;
    }
    
    var w = infovis.offsetWidth+70, h = infovis.offsetHeight-100;
    


    //init Hypertree
    var ht = new $jit.Hypertree({
      //id of the visualization container
      injectInto: cont,
      //canvas width and height
      width: w,
      height: h,
      //Change node and edge styles such as
      //color, width and dimensions.
      Node: {
          dim: 9,
          color: "#9F1326",
          type:"none"
      },
      Edge: {
          lineWidth: 2,
          color: "#142F4F",
          overridable: "true"
          //type: "myLine"
      },

      //Attach event handlers and add text to the
      //labels. This method is only triggered on label
      //creation
      onCreateLabel: function(domElement, node){
          domElement.innerHTML = node.name;
          $jit.util.addEvent(domElement, 'click', function () {
              ht.onClick(node.id);
              //var test1 = node.getSubnodes();
              sleep(600);
          });
      },
      //Change node styles when labels are placed
      //or moved.
      onPlaceLabel: function(domElement, node){
          var style = domElement.style;
          style.display = '';
          style.cursor = 'pointer';
          if (node._depth <= 1) {
              style.fontSize = "0.8em";
              style.color = "#ddd";

          } else if(node._depth == 2){
              style.fontSize = "0.7em";
              style.color = "#555";

          } else {
              style.display = 'none';
          }

          var left = parseInt(style.left);
          var w = domElement.offsetWidth;
          style.left = (left - w / 2) + 'px';
      },
      
      onAfterCompute: function(){

          //Build the right column relations list.
          //This is done by collecting the information (stored in the data property) 
          //for all the nodes adjacent to the centered node.
          var node = ht.graph.getClosestNodeToOrigin("current");
          var html = "<h4>" + node.name + "</h4><b>Connections:</b>";
          html += "<ul>";
          node.eachAdjacency(function(adj){
              var child = adj.nodeTo;
              if (child.data) {
                  var rel = (child.data.band == node.name) ? child.data.relation : node.data.relation;
                  html += "<li>" + child.name + " " + "<div class=\"relation\">(relation: " + rel + ")</div></li>";
              }
          });
          html += "</ul>";
         //$jit.id('inner-details').innerHTML = html;

      }
    });

    //load JSON data.
    ht.loadJSON(json);
    //compute positions and plot.
    ht.refresh();
    //end
    ht.controller.onAfterCompute();
}
//#####################################################################//
//--------------------------------------------------------RGraph--------------------------------------------------------//
//######################################################################//
function initRGraph(input1, id1){
    //init data
    var cont = id1;
    var json = input1;
    //end
    var infovis = document.getElementById(cont);
    var w = infovis.offsetWidth + 150, h = infovis.offsetHeight - 50;

    //init RGraph
    var rgraph = new $jit.RGraph({
        //Where to append the visualization
        injectInto: cont,
        width: w,
        height: h,

        //Optional: create a background canvas that plots
        //concentric circles.
        background: {
          CanvasStyles: {
            strokeStyle: '#555'
          }
        },
        //Add navigation capabilities:
        //zooming by scrolling and panning.
        Navigation: {
          enable: true,
          panning: true,
          zooming: 10
        },
        //Set Node and Edge styles.
        Node: {
            color: '#ddeeff'
        },
        
        Edge: {
          color: '#C17878',
          lineWidth:1.5
        },

        onBeforeCompute: function(node){
            //Add the relation list in the right column.
            //This list is taken from the data property of each JSON node.
        },
        
        onAfterCompute: function(){
        },
        //Add the name of the node in the correponding label
        //and a click handler to move the graph.
        //This method is called once, on label creation.
        onCreateLabel: function(domElement, node){
            domElement.innerHTML = node.name;
            domElement.onclick = function(){
                rgraph.onClick(node.id);
                sleep(1000);
//              window.location=node.id;
            };
        },
        //Change some label dom properties.
        //This method is called each time a label is plotted.
        onPlaceLabel: function(domElement, node){
            var style = domElement.style;
            style.display = '';
            style.cursor = 'pointer';

            if (node._depth <= 1) {
                style.fontSize = "0.8em";
                style.color = "#ccc";
            
            } else if(node._depth == 2){
                style.fontSize = "0.7em";
                style.color = "#494949";
            
            } else {
                style.fontSize = "0.7em";
                style.color = "#494949";
            }

            var left = parseInt(style.left);
            var w = domElement.offsetWidth;
            style.left = (left - w / 2) + 'px';
        }
    });
    //load JSON data
    rgraph.loadJSON(json);
    //trigger small animation
    rgraph.graph.eachNode(function(n) {
      var pos = n.getPos();
      pos.setc(-200, -200);
    });
    rgraph.compute('end');
    rgraph.fx.animate({
      modes:['polar'],
      duration: 10
    });
    //end
}
//#####################################################################//
//--------------------------------------------------------Spacetree----------------------------------------------------//
//######################################################################//
function initSpacetree(input1, id1){
    //init data
    var cont = id1;
    var json = input1;
    //end
    //init Spacetree
    //Create a new ST instance
    var st = new $jit.ST({
        //id of viz container element
        injectInto: cont,
        orientation: 'left',
        //set duration for the animation
        duration: 200,
        //set animation transition type
        transition: $jit.Trans.Quart.easeInOut,
        //set distance between node and its children
        levelDistance: 80,
        //enable panning
        Navigation: {
          enable:true,
          panning:true
        },
        //set node and edge styles
        //set overridable=true for styling individual
        //nodes or edges
        Node: {
            height: 60,
            width: 100,
            type: 'ellipse',
            color: 'white',
            overridable: false
        },
        
        Edge: {
            type: 'bezier',
            lineWidth: 1,
            color:'#23A4FF',
            overridable: true
        },
        
        onBeforeCompute: function(node){
        },
        
        onAfterCompute: function(){
        },
        Tips: {
            enable: true,
            //add positioning offsets
            offsetX: 20,
            offsetY: 20,
            //implement the onShow method to
            //add content to the tooltip when a node
            //is hovered
            onShow: function(tip, node, isLeaf, domElement) {
              var html ="<div style='border:1px solid #C9C9CA;padding:5px;background-color:#ffffff;opacity:0.7;' class=\"node-id\">";
              if(node.id.indexOf('http') == -1)
             	 html += node.name +"</div>";
              else
                html += node.id.substring(node.id.indexOf('http'))+"</div>";
              tip.innerHTML =  html; 
            }  
          },
        //This method is called on DOM label creation.
        //Use this method to add event handlers and styles to
        //your node.
        onCreateLabel: function(label, node){
            label.id = node.id;     
            if(node.data.img){
            var html = "<div style='margin-top:5px;'><span style = 'left-margin:20px;'><img width=\"30px\" height=\"30\" src="+node.data.img+" /></span>";
              html += "<span  class=\"node-name\">" + node.name.replace('href','class')
                + "</span></div>";
              }else{
                  var html = "<div style='margin-top:15px;' class=\"node-name\">" + node.name.replace('href','class')
                  + "</div>";
              }
            label.innerHTML = html;
            
            label.onclick = function(){

                  st.onClick(node.id);

            };
            //set label styles
            var style = label.style;

            style.width = 100 + 'px';
            style.height = '40';
            style.cursor = 'pointer';
            style.color = '#333';
            style.fontSize = '0.8em';
            style.textAlign= 'center';
            style.verticalAlign= 'bottom';
            style.paddingTop = '0px';
            style.marginLeft = '5px';           
            style.marginTop ='5px';
        },
        
        //This method is called right before plotting
        //a node. It's useful for changing an individual node
        //style properties before plotting it.
        //The data properties prefixed with a dollar
        //sign will override the global node style properties.
        onBeforePlotNode: function(node){
            //add some color to the nodes in the path between the
            //root node and the selected node.
            if (node.selected) {
                node.data.$color = "#ff7";
            }
            else {
                delete node.data.$color;
                //if the node belongs to the last plotted level
                if(!node.anySubnode("exist")) {
                    //count children number
                    var count = 0;
                    node.eachSubnode(function(n) { count++; });
                    //assign a node color based on
                    //how many children it has
                    node.data.$color = ['#aaa', '#baa', '#caa', '#daa', '#eaa', '#faa'][count];                    
                }
            }
        },
        
        //This method is called right before plotting
        //an edge. It's useful for changing an individual edge
        //style properties before plotting it.
        //Edge data proprties prefixed with a dollar sign will
        //override the Edge global style properties.
        onBeforePlotLine: function(adj){
            if (adj.nodeFrom.selected && adj.nodeTo.selected) {
                adj.data.$color = "#79797C";
                adj.data.$lineWidth = 1;
            }
            else {
                delete adj.data.$color;
                delete adj.data.$lineWidth;
            }
        }
    });
    //load json data
    st.loadJSON(json);
    //compute node positions and layout
    st.compute();
    //optional: make a translation of the tree
    st.geom.translate(new $jit.Complex(-200, 0), "current");
    //emulate a click on the root node.
    st.onClick(st.root);
    //end

}
//#####################################################################//
//---------------------------------------------ForceDirectedGraph------------------------------------------------//
//######################################################################//
function initForceDirectedGraph(input1, id1){
      // init data
    var cont = id1;
    var json = input1.array;
    //label placement on edges
      $jit.ForceDirected.Plot.EdgeTypes.implement({
      'labeled': { 'render': function(adj, canvas) {
      //plot arrow edge
     if(adj){
     this.edgeTypes.line.render.call(this, adj, canvas); 
      //get nodes cartesian coordinates

      var pos = adj.nodeFrom.pos.getc(true);
      var posChild = adj.nodeTo.pos.getc(true);
      //check for edge label in data
      var data = adj.data;
      if(data.labeltext) {
         //now adjust the label placement
          var radius = this.viz.canvas.getSize();
          var x = parseInt((pos.x + posChild.x - (data.labeltext.length * 5)) /
          2);
          var y = parseInt((pos.y + posChild.y ) /2);
          this.viz.canvas.getCtx().fillText(data.labeltext, x, y);  
      }}}}});
      // init ForceDirected
      var fd = new $jit.ForceDirected({
        //id of the visualization container
        injectInto: cont,
        //Enable zooming and panning
        //with scrolling and DnD
        Navigation: {
          enable: true,
          type: 'Native',
          //Enable panning events only if we're dragging the empty
          //canvas (and not a node).
          panning: 'avoid nodes',
          zooming: 10 //zoom speed. higher is more sensible
        },
        // Change node and edge styles such as
        // color and width.
        // These properties are also set per node
        // with dollar prefixed data-properties in the
        // JSON structure.
        Node: {
          overridable: true,
          dim: 8
//        type: 'pics'
        },
        Edge: {
          overridable: true,
          color: '#7D737C',
          lineWidth: 0.4,
          type: 'labeled'
        },
        // Add node events
        Events: {
          enable: true,
          type: 'Native',
          //Change cursor style when hovering a node
          onMouseEnter: function() {
            fd.canvas.getElement().style.cursor = 'move';
          },
          onMouseLeave: function() {
            fd.canvas.getElement().style.cursor = '';
          },
          //Update node positions when dragged
          onDragMove: function(node, eventInfo, e) {
            var pos = eventInfo.getPos();
            node.pos.setc(pos.x, pos.y);
            fd.plot();
          },
          //Implement the same handler for touchscreens
          onTouchMove: function(node, eventInfo, e) {
            $jit.util.event.stop(e); //stop default touchmove event
            this.onDragMove(node, eventInfo, e);
          }
        },
        //Number of iterations for the FD algorithm
        iterations: 200,
        //Edge length
        levelDistance: 130,
        // This method is only triggered
        // on label creation and only for DOM labels (not native canvas ones).
        onCreateLabel: function(domElement, node){
          // Create a 'name' and 'close' buttons and add them
          // to the main node label
          var nameContainer = document.createElement('span'),
              closeButton = document.createElement('span'),
              style = nameContainer.style;
          nameContainer.className = 'name';
          nameContainer.innerHTML = "<span class=\"name\">" + node.name + "</span>"
          closeButton.className = 'close';
          closeButton.innerHTML = '     x';
          closeButton.style.color = 'red';
          closeButton.style.fontSize = "0.8em";
          closeButton.onmouseover = function(){
              this.style.cursor='pointer';
          }
          domElement.appendChild(nameContainer);
          domElement.appendChild(closeButton);
          style.fontSize = "0.8em";
          style.color = "black";
          //Fade the node and its connections when
          //clicking the close button
          closeButton.onclick = function() {
            node.setData('alpha', 0, 'end');
//          node.img(function(img){
//                img.setData('alpha', 0, 'end'); });
            node.eachAdjacency(function(adj) {
              adj.setData('alpha', 0, 'end');
            });
            fd.fx.animate({
              modes: ['node-property:alpha',
                      'edge-property:alpha'],
              duration: 500
            });
          };
          //Toggle a node selection when clicking
          //its name. This is done by animating some
          //node styles like its dimension and the color
          //and lineWidth of its adjacencies.
          nameContainer.onclick = function() {
            //set final styles
            fd.graph.eachNode(function(n) {
              if(n.id != node.id) delete n.selected;
              n.setData('dim', 9, 'end');
              n.eachAdjacency(function(adj) {
                adj.setDataset('end', {
                  lineWidth: 0.4,
                  color: '#968A95'
                });
              });
            });
            if(!node.selected) {
              node.selected = true;
              node.setData('dim', 17, 'end');
              node.eachAdjacency(function(adj) {
                adj.setDataset('end', {
                  lineWidth: 2,
                  color: '#371D9F'
                });
              });
            } else {
              delete node.selected;
            }
            //trigger animation to final styles
            fd.fx.animate({
              modes: ['node-property:dim',
                      'edge-property:lineWidth:color'],
              duration: 500
            });
          };
        },
        // Change node styles when DOM labels are placed
        // or moved.
        onPlaceLabel: function(domElement, node){
          var style = domElement.style;
          var left = parseInt(style.left);
          var top = parseInt(style.top);
          var w = domElement.offsetWidth;
          style.left = (left - w / 2) + 'px';
          style.top = (top + 10) + 'px';
//        style.display = '';
        }
      });
      // load JSON data.
      fd.loadJSON(json);
      // compute positions incrementally and animate.
      fd.computeIncremental({
        iter: 40,
        property: 'end',
        onStep: function(perc){
          // Log.write(perc + '% loaded...');
        },
        onComplete: function(){
          // Log.write('done');
          fd.animate({
            modes: ['linear'],
            transition: $jit.Trans.Elastic.easeOut,
            duration: 2500
          });
        }
      });
}
      // end
//#####################################################################//
//------------------------------ForceDirectedGraph with icon nodes --------------------------------------//
//######################################################################//
      function initIconsForceDirectedGraph(input1, id1){
          // init data
          var cont = id1;
          var json = input1.array;
          // end
          //nodes as icons
          $jit.ForceDirected.Plot.NodeTypes.implement({
              'pics': {
                    'render': function(node, canvas) {
                            if(node.data.img)
                            {
                                //if transparency is set to 100, node will not be rendered
                                if(node.imgTransparency == 100)
                                    return;
                                var ctx = canvas.getCtx();
                                var pos = node.pos.getc(true);
//                              var img = node.getData('successful') ? $('successImage') : $('errorImage');
                                var img = new Image();
                                img.src = node.data.img;
                                var sizex = 30;
                                var sizey = 30;
                                var xpos = pos.x;
                                var ypos = pos.y;
    
                                try{
                                ctx.drawImage(img, xpos-sizex/2, ypos-sizey/2, sizex, sizey);
                                }catch(e){
                                    this.nodeTypes.circle.render.call(this, node, canvas); 
                                }
                            }
                            else
                            {    
//                              var ntype = node.getData('$type');
                                this.nodeTypes.circle.render.call(this, node, canvas); 
//                              node.overrride = 'true';
//                              node.type =node.data.$type;
//                              node.dim = node.data.$dim;
//                              node.color = node.data.$color;

                            }
                    },
                    'contains': function(node, pos) {
                            var npos = node.pos.getc(true);
//                          var img = new Image();

               var width = 30,
                    height = 30;

                return Math.abs(pos.x - npos.x) <= width / 2
                && Math.abs(pos.y - npos.y) <= height / 2;
                    }
              }
        });   
        //label placement on edges
          $jit.ForceDirected.Plot.EdgeTypes.implement({
          'labeled': { 'render': function(adj, canvas) {
          //plot arrow edge
         this.edgeTypes.line.render.call(this, adj, canvas); 
          //get nodes cartesian coordinates
          var pos = adj.nodeFrom.pos.getc(true);
          var posChild = adj.nodeTo.pos.getc(true);
          //check for edge label in data
          var data = adj.data;
          if(data.labeltext) {
             //now adjust the label placement
              var radius = this.viz.canvas.getSize();
              var x = parseInt((pos.x + posChild.x - (data.labeltext.length * 5)) /
              2);
              var y = parseInt((pos.y + posChild.y ) /2);
              this.viz.canvas.getCtx().fillText(data.labeltext, x, y);  
          }}}});
         //zoom should be forbidden in IE8 to avoid side effects
          var zoom=20;
          if(getMsieVersion()==8)
          {
             zoom = 0;
          }
          // init ForceDirected
          var fd = new $jit.ForceDirected({
            //id of the visualization container
            injectInto: cont,
            //Enable zooming and panning
            //with scrolling and DnD
            Navigation: {
              enable: true,
              type: 'Native',
              //Enable panning events only if we're dragging the empty
              //canvas (and not a node).
              panning: 'avoid nodes',
              zooming: zoom //zoom speed. higher is more sensible
            },
            // Change node and edge styles such as
            // color and width.
            // These properties are also set per node
            // with dollar prefixed data-properties in the
            // JSON structure.
            Node: {
              overridable: false,
              dim: 8,
              type: 'pics'
            },
            Edge: {
              overridable: true,
              color: '#000000',
              lineWidth: 0.5,
              type: 'labeled'
            },
            //Add Tips
            Tips: {
              enable: true,
              onShow: function(tip, node) {
                //count connections
                var count = 0;
                node.eachAdjacency(function() { count++; });
                //display node info in tooltip
                tip.innerHTML = "<div class=\"tip-title\">" + node.name + "</div>"
                  + "<div class=\"tip-text\"><b>connections:</b> " + count + "</div>";
              }
            },
            // Add node events
            Events: {
              enable: true,
              type: 'Native',
              //Change cursor style when hovering a node
              onMouseEnter: function() {
                fd.canvas.getElement().style.cursor = 'move';
              },
              onMouseLeave: function() {
                fd.canvas.getElement().style.cursor = '';
              },
              //Update node positions when dragged
              onDragMove: function(node, eventInfo, e) {
                var pos = eventInfo.getPos();
                node.pos.setc(pos.x, pos.y);
                fd.plot();
              },
              //Implement the same handler for touchscreens
              onTouchMove: function(node, eventInfo, e) {
                $jit.util.event.stop(e); //stop default touchmove event
                this.onDragMove(node, eventInfo, e);
              }
            },
            //Number of iterations for the FD algorithm
            iterations: 200,
            //Edge length
            levelDistance: 130,
            // This method is only triggered
            // on label creation and only for DOM labels (not native canvas ones).
            onCreateLabel: function(domElement, node){
              // Create a 'name' and 'close' buttons and add them
              // to the main node label
              var nameContainer = document.createElement('span'),
                  closeButton = document.createElement('span'),
                  style = nameContainer.style;
              nameContainer.className = 'name';
              nameContainer.innerHTML = node.name;
              closeButton.className = 'close';
              closeButton.innerHTML = '     x';
              closeButton.style.color = 'red';
              closeButton.style.fontSize = "0.8em";
              closeButton.onmouseover = function(){
                  this.style.cursor='pointer';
              }
              domElement.appendChild(nameContainer);
              domElement.appendChild(closeButton);
              style.fontSize = "0.8em";
              //Fade the node and its connections when
              //clicking the close button
              closeButton.onclick = function() {
                node.setData('alpha', 0);
                //set attribute to hide the node --> setData does not work here (overridable=false)
                node.imgTransparency = 100;
                
                //hide label
                fd.labels.disposeLabel(node.id);
                node.eachAdjacency(function(adj) {
                  adj.setData('alpha', 0, 'end');
                });
                fd.fx.animate({
                  modes: ['node-property:alpha',
                          'edge-property:alpha'],
                  duration: 500
                });
              };
              //Toggle a node selection when clicking
              //its name. This is done by animating some
              //node styles like its dimension and the color
              //and lineWidth of its adjacencies.
              nameContainer.onclick = function() {
                //set final styles
                fd.graph.eachNode(function(n) {
                  if(n.id != node.id) delete n.selected;
                  n.setData('dim', 9, 'end');
                  n.eachAdjacency(function(adj) {
                    adj.setDataset('end', {
                      lineWidth: 0.4,
                      color: '#968A95'
                    });
                  });
                });
                if(!node.selected) {
                  node.selected = true;
                  node.setData('dim', 17, 'end');
                  node.eachAdjacency(function(adj) {
                    adj.setDataset('end', {
                      lineWidth: 2,
                      color: '#E831D0'
                    });
                  });
                } else {
                  delete node.selected;
                }
                //trigger animation to final styles
                fd.fx.animate({
                  modes: ['node-property:dim',
                          'edge-property:lineWidth:color'],
                  duration: 500
                });
                // Build the right column relations list.
                // This is done by traversing the clicked node connections.
                var html = "<h4>" + node.name + "</h4><b> connections:</b><ul><li>",
                    list = [];
                node.eachAdjacency(function(adj){
                  if(adj.getData('alpha')) list.push(adj.nodeTo.name);
                });
                //append connections information
//              $jit.id('inner-details').innerHTML = html + list.join("</li><li>") + "</li></ul>";
              };
            },
            // Change node styles when DOM labels are placed
            // or moved.
            onPlaceLabel: function(domElement, node){
              var style = domElement.style;
              var left = parseInt(style.left);
              var top = parseInt(style.top);
              var w = domElement.offsetWidth;
              style.left = (left - w / 2) + 'px';
              style.top = (top + 10) + 'px';
//            style.display = '';
            }
          });
          // load JSON data.
          fd.loadJSON(json);
          // compute positions incrementally and animate.
          fd.computeIncremental({
            iter: 40,
            property: 'end',
            onStep: function(perc){
              // Log.write(perc + '% loaded...');
            },
            onComplete: function(){
              // Log.write('done');
              fd.animate({
                modes: ['linear'],
                transition: $jit.Trans.Elastic.easeOut,
                duration: 2500
              });
            }
          });
          // end
    }
    //#####################################################################//
    //------------------------------Class Tree --------------------------------------//
    //######################################################################//
      function initClassTree(input1, id1){
            //init data
            var cont = id1;
            var json = input1;
            //end
            //init Spacetree
            //Create a new ST instance
            var st = new $jit.ST({
                //id of viz container element
                injectInto: cont,
                orientation: 'left',
                //set duration for the animation
                duration: 200,
                //set animation transition type
                transition: $jit.Trans.Quart.easeInOut,
                //set distance between node and its children
                levelDistance: 50,
                //enable panning
                Navigation: {
                  enable:true,
                  panning:true
                },
                //set node and edge styles
                //set overridable=true for styling individual
                //nodes or edges
                Node: {
                    height: 50,
                    width: 90,
                    type: 'ellipse',
                    color: '#aaa',
                    overridable: true
                },
                
                Edge: {
                    type: 'bezier',
                    overridable: true
                },
                
                onBeforeCompute: function(node){
                },
                
                onAfterCompute: function(){
                },
                Tips: {
                    enable: true,
                    //add positioning offsets
                    offsetX: 20,
                    offsetY: 20,
                    //implement the onShow method to
                    //add content to the tooltip when a node
                    //is hovered
                    onShow: function(tip, node, isLeaf, domElement) {
                      var html ="</div><div class=\"node-id\">";
                        html += node.id.substring(node.id.indexOf('http'));
                      tip.innerHTML =  html; 
                    }  
                  },
                //This method is called on DOM label creation.
                //Use this method to add event handlers and styles to
                //your node.
                onCreateLabel: function(label, node){
                    label.id = node.id;            
                    label.innerHTML = node.name;
                    label.onclick = function(){

                          st.onClick(node.id);

                    };
                    //set label styles
                    var style = label.style;
                    style.width = 80 + 'px';
                    style.height = 40 + 'px';
                    style.cursor = 'pointer';
                    style.color = '#333';
                    style.fontSize = '0.8em';
                    style.textAlign= 'center';
                    style.paddingTop = '10px';
                    style.marginLeft = '5px';
                },
                
                //This method is called right before plotting
                //a node. It's useful for changing an individual node
                //style properties before plotting it.
                //The data properties prefixed with a dollar
                //sign will override the global node style properties.
                onBeforePlotNode: function(node){
                    //add some color to the nodes in the path between the
                    //root node and the selected node.
                    if (node.selected) {
                        node.data.$color = "#ff7";
                    }
                    else {
                        delete node.data.$color;
                        //if the node belongs to the last plotted level
                        if(!node.anySubnode("exist")) {
                            //count children number
                            var count = 0;
                            node.eachSubnode(function(n) { count++; });
                            //assign a node color based on
                            //how many children it has
                            node.data.$color = ['#aaa', '#baa', '#caa', '#daa', '#eaa', '#faa'][count];                    
                        }
                    }
                },
                
                //This method is called right before plotting
                //an edge. It's useful for changing an individual edge
                //style properties before plotting it.
                //Edge data proprties prefixed with a dollar sign will
                //override the Edge global style properties.
                onBeforePlotLine: function(adj){
                    if (adj.nodeFrom.selected && adj.nodeTo.selected) {
                        adj.data.$color = "#eed";
                        adj.data.$lineWidth = 3;
                    }
                    else {
                        delete adj.data.$color;
                        delete adj.data.$lineWidth;
                    }
                }
            });
            //load json data
            st.loadJSON(json);
            //compute node positions and layout
            st.compute();
            //optional: make a translation of the tree
            st.geom.translate(new $jit.Complex(-200, 0), "current");
            //emulate a click on the root node.
            st.onClick(st.root);
            //end

        }
