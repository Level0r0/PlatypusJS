/* global Java */

define(function () {
    var LoggerClass = Java.type("java.util.logging.Logger");
    var applicationLogger = LoggerClass.getLogger("Application");
    var Logger = {
        /**
         * 
         * @param {type} aMessage
         * @returns {undefined}
         */
        info: function(aMessage){},
        /**
         * 
         * @param {type} aMessage
         * @returns {undefined}
         */
        config: function(aMessage){},
        /**
         * 
         * @param {type} aMessage
         * @returns {undefined}
         */
        fine: function(aMessage){},
        /**
         * 
         * @param {type} aMessage
         * @returns {undefined}
         */
        finer: function(aMessage){},
        /**
         * 
         * @param {type} aMessage
         * @returns {undefined}
         */
        finest: function(aMessage){},
        /**
         * 
         * @param {type} aMessage
         * @returns {undefined}
         */
        severe: function(aMessage){},
        /**
         * 
         * @param {type} aMessage
         * @returns {undefined}
         */
        warning: function(aMessage){}
    };
    Object.defineProperty(Logger, "config", {value: function (aMessage) {
            applicationLogger.config("" + aMessage);
        }});
    Object.defineProperty(Logger, "severe", {value: function (aMessage) {
            applicationLogger.severe("" + aMessage);
        }});
    Object.defineProperty(Logger, "warning", {value: function (aMessage) {
            applicationLogger.warning("" + aMessage);
        }});
    Object.defineProperty(Logger, "info", {value: function (aMessage) {
            applicationLogger.info("" + aMessage);
        }});
    Object.defineProperty(Logger, "fine", {value: function (aMessage) {
            applicationLogger.fine("" + aMessage);
        }});
    Object.defineProperty(Logger, "finer", {value: function (aMessage) {
            applicationLogger.finer("" + aMessage);
        }});
    Object.defineProperty(Logger, "finest", {value: function (aMessage) {
            applicationLogger.finest("" + aMessage);
        }});
    return Logger;
});