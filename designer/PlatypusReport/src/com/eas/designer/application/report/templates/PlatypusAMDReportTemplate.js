/**
 * 
 * @author ${user}
 */
define('${appElementName}', ['orm', 'template'], function (Orm, loadTemplate, ModuleName) {
    function module_constructor() {
        var self = this
                , model = Orm.loadModel(ModuleName)
                , template = loadTemplate(ModuleName, model);

        // TODO : place constructor code here

        self.execute = function (onSuccess, onFailure) {

            model.requery(function () {
                // TODO : place data processing code here
                var report = template.generateReport();
                // report.show(); | report.print(); | var savedTo = report.save(saveTo ?);
                onSuccess(report);
            }, onFailure);

        };
    }
    return module_constructor;
});
