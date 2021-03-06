/* global Java */

define(['boxing'], function(B) {
    var className = "com.eas.client.changes.Update";
    var javaClass = Java.type(className);
    /**
     * Generated constructor.
     * @constructor Update Update
     */
    function Update() {
        var maxArgs = 0;
        var delegate = arguments.length > maxArgs ?
              arguments[maxArgs] 
            : new javaClass();

        Object.defineProperty(this, "unwrap", {
            configurable: true,
            value: function() {
                return delegate;
            }
        });
        if(Update.superclass)
            Update.superclass.constructor.apply(this, arguments);
        delegate.setPublished(this);
        /**
         * Data to be applied within a target datasource.
         */
        this.data = new Object();
        Object.defineProperty(this, "data", {
            get: function() {
                var value = delegate.data;
                return B.boxAsJs(value);
            }
        });

        /**
         * Keys used for indentifying data changes within a target datasource
         */
        this.keys = new Object();
        Object.defineProperty(this, "keys", {
            get: function() {
                var value = delegate.keys;
                return B.boxAsJs(value);
            }
        });

        /**
         * Indicates the change's type (Insert, Update, Delete or Command).
         */
        this.type = '';
        Object.defineProperty(this, "type", {
            get: function() {
                var value = delegate.type;
                return B.boxAsJs(value);
            }
        });

        /**
         *Indicates the change's destination entity.
         */
        this.entity = '';
        Object.defineProperty(this, "entity", {
            get: function() {
                var value = delegate.entity;
                return B.boxAsJs(value);
            }
        });

    }

    var ScriptsClass = Java.type("com.eas.script.Scripts");
    var space = ScriptsClass.getSpace();
    space.putPublisher(className, function(aDelegate) {
        return new Update(aDelegate);
    });
    return Update;
});