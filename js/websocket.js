(function () {

	var global = window;
	
	var WebSocket = global.WebSocket = function (url) {
		// listener to overload
		this.onopen = null;
		this.onmessage = null;
		this.onerror = null;
		this.onclose = null;
		
		this._handler = WebSocketFactory.getNew(url);
		WebSocket.registry[this._handler.getIdentifier()] = this;
		
		this.readyState = WebSocket.CONNECTING;
	};
	
	WebSocket.registry = {};
	
	WebSocket.triggerEvent = function (evt) {
		WebSocket.__open
		WebSocket.registry[evt.target]['on' + evt.type].call(global, evt);
		if (evt.type == 'open') WebSocket.registry[evt.target].pollJS(evt);
	}
	
	WebSocket.prototype.send = function (data) 
	{
		this._handler.send(data);
	}
	
	WebSocket.prototype.close = function () 
	{
		this._handler.close(data);
	}

	WebSocket.prototype.pollJS = function (evt)
	{
		try{
			var strCode = this._handler.getPendingJS();
			if (strCode != 'javascript: ') (new Function(strCode))();
			if (evt.type != 'close' && evt.type != 'error') setInterval(function(){WebSocket.registry[evt.target].pollJS(evt)},3000);
		} catch (error) {
			console.log("pollJS ERROR: [" + error + "]");
		}

	}

})();