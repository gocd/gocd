import * as Routes from "gen/ts-routes";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import * as Buttons from "views/components/buttons";
import * as css from "./components.scss";

interface Attrs {
  pipelineConfig: PipelineConfig;
}

export class PipelineActions extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    return (
      <footer class={css.actions}>
        <Buttons.Cancel onclick={this.onCancel.bind(this)} small={false}>Cancel</Buttons.Cancel>
        <div class={css.saveBtns}>
          <span class={css.errorResponse}></span>
          <Buttons.Secondary onclick={this.onSave.bind(this, true, vnode.attrs.pipelineConfig)} small={false}>Save + Edit Full Config</Buttons.Secondary>
          <Buttons.Primary onclick={this.onSave.bind(this, false, vnode.attrs.pipelineConfig)} small={false}>Save + Run This Pipeline</Buttons.Primary>
        </div>
      </footer>
    );
  }

  onCancel(event: Event): void {
    event.stopPropagation();
    window.location.href = "/go/pipelines";
  }

  onSave(shouldPause: boolean, pipelineConfig: PipelineConfig, event: Event): void {
    event.stopPropagation();
    this.clearError();

    if (pipelineConfig.isValid()) {
      pipelineConfig.create().then((response) => {
        response.getOrThrow();
        if (shouldPause) {
          pipelineConfig.pause().then(() => {
            window.location.href = Routes.pipelineEditPath("pipelines", pipelineConfig.name(), "general");
          });
        } else  {
          window.location.href = "/go/pipelines";
        }
      }).catch((reason) => {
        this.setError(reason);
      });
    }
  }

  private clearError(): Node {
    return empty(document.querySelector(`.${css.errorResponse}`)!);
  }

  private setError(text: string) {
    this.clearError().textContent = text;
  }
}

function empty(el: Node) {
  while (el.firstChild) {
    el.removeChild(el.firstChild);
  }
  return el;
}
