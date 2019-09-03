export interface LocationHandler {
  go: (url: string) => void;
}

export class WindowLocation implements LocationHandler {
  go(url: string) {
    window.location.href = url;
  }
}
