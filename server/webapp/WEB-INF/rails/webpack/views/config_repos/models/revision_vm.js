const m           = require("mithril");
const Stream      = require("mithril/stream");
const ApiHelper   = require("helpers/api_helper");
const AjaxPoller  = require("helpers/ajax_poller");

import SparkRoutes from '../../../helpers/spark_routes';

const apiVersion = "v1";

function RevisionVM(repoId) {
  this.id           = repoId;
  this.busy         = Stream(false);
  this.serverErrors = Stream(null);
  this.revision     = Stream(null);
  this.error        = Stream(null);
  this.success      = Stream(false);

  const poller = createPoller(this);

  this.reload = () => {
    this.serverErrors(null);
    this.busy(true);

    const req = ApiHelper.GET({
      url: SparkRoutes.configRepoLastParsedResultPath(repoId),
      apiVersion
    });

    req.always(() => this.busy(false));
    req.then((data) => {
      this.revision(data.revision);
      this.success(data.success);
      this.error(data.error);
    }, this.serverErrors);

    return req;
  };

  this.forceUpdate = () => {
    this.serverErrors(null);
    this.busy(true);

    const req = ApiHelper.POST({
      url: SparkRoutes.configRepoTriggerUpdatePath(repoId),
      apiVersion
    });

    req.then(this.monitorProgress, (errMsg) => {
      this.serverErrors(errMsg);
      this.busy(false);
    });
    return req;
  };

  this.monitorProgress = poller.start;
}

function createPoller(model) {
  const poller = new AjaxPoller({
    intervalSeconds: 2,
    fn: () => ApiHelper.GET({
      url: SparkRoutes.configRepoRevisionStatusPath(model.id),
      apiVersion
    }).then((data) => {
      if (!data.inProgress) {
        poller.stop();
        model.reload().always(m.redraw);
      }
    }, (errMsg) => {
      poller.stop();
      model.busy(false);
      model.serverErrors(errMsg);
    })
  });

  return poller;
}

module.exports = RevisionVM;
