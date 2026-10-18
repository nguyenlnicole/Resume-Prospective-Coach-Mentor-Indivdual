const {
  app,
  BrowserWindow,
  Menu,
  dialog,
  net,
  protocol,
  session,
  shell,
} = require("electron");
const path = require("node:path");
const { pathToFileURL } = require("node:url");
const {
  origin,
  csp,
  assetPath,
  externalUrl,
  resumeDownload,
  createApiProxy,
} = require("./transport.cjs");
const config = require("./desktop-config.json");

const edition = ["coach", "beta"].includes(config.edition)
  ? config.edition
  : "coach";
app.setName(
  `PNB Resume Prospective ${edition === "beta" ? "Individual" : "Coach & Mentor"}`,
);
app.setPath(
  "userData",
  path.join(app.getPath("appData"), `PNB-Resume-Prospective-${edition}`),
);
if (process.env.PNB_DESKTOP_PROFILE) {
  const profile = path.resolve(process.env.PNB_DESKTOP_PROFILE);
  require("node:fs").mkdirSync(profile, { recursive: true });
  app.setPath("userData", profile);
}
app.setAppUserModelId(`com.pnbpaws.resumeprospective.${edition}`);
protocol.registerSchemesAsPrivileged([
  {
    scheme: "pnb-app",
    privileges: {
      standard: true,
      secure: true,
      supportFetchAPI: true,
      stream: true,
    },
  },
]);
let window;
if (!app.requestSingleInstanceLock()) app.quit();
else {
  app.on("second-instance", () => {
    if (window) {
      if (window.isMinimized()) window.restore();
      window.focus();
    }
  });
  app
    .whenReady()
    .then(start)
    .catch(() => {
      dialog.showErrorBox(
        "PNB Resume Prospective",
        "The desktop application could not start. Please reinstall or contact the app owner.",
      );
      app.quit();
    });
}

async function start() {
  const root = path.join(__dirname, "ui");
  const proxy = createApiProxy(config);
  session.defaultSession.setPermissionRequestHandler(
    (_contents, _permission, callback) => callback(false),
  );
  session.defaultSession.setPermissionCheckHandler(() => false);
  protocol.handle("pnb-app", async (request) => {
    const url = new URL(request.url);
    if (`${url.protocol}//${url.host}` !== origin)
      return new Response(null, { status: 403 });
    if (url.pathname === "/desktop-info")
      return Response.json({ preview: config.preview, desktop: true });
    if (url.pathname.startsWith("/api/")) return proxy(request);
    const file = assetPath(root, request.url);
    if (!file || request.method !== "GET")
      return new Response(null, { status: 404 });
    try {
      const response = await net.fetch(pathToFileURL(file).toString());
      const headers = new Headers(response.headers);
      headers.set("Content-Security-Policy", csp);
      headers.set("X-Content-Type-Options", "nosniff");
      return new Response(response.body, { status: response.status, headers });
    } catch {
      return new Response(null, { status: 404 });
    }
  });
  session.defaultSession.on("will-download", (event, item) => {
    const file = resumeDownload(
      item.getURL(),
      item.getMimeType(),
      item.getFilename(),
    );
    if (!file) {
      event.preventDefault();
      return;
    }
    item.setSaveDialogOptions({
      title: "Save your resume",
      defaultPath: path.join(app.getPath("documents"), file.name),
      filters: [{ name: file.label, extensions: [file.extension] }],
    });
  });
  Menu.setApplicationMenu(
    Menu.buildFromTemplate([
      ...(process.platform === "darwin" ? [{ role: "appMenu" }] : []),
      { label: "File", submenu: [{ role: "quit" }] },
      { role: "editMenu" },
      {
        label: "View",
        submenu: [
          { role: "resetZoom" },
          { role: "zoomIn" },
          { role: "zoomOut" },
          { type: "separator" },
          { role: "togglefullscreen" },
        ],
      },
      { role: "windowMenu" },
      {
        label: "Help",
        submenu: [
          {
            label: "About PNB Resume Prospective",
            click: () =>
              dialog.showMessageBox(window, {
                type: "info",
                title: "PNB Resume Prospective",
                message: "PNB Resume Prospective",
                detail: `Version ${app.getVersion()}${config.preview ? " · Local preview" : ""}\nA software product by PNB Paws LLC.\nInternet access is required for resume reviews, email, and coach access.`,
              }),
          },
        ],
      },
    ]),
  );
  openWindow();
}

function openWindow() {
  window = new BrowserWindow({
    width: 1450,
    height: 1040,
    minWidth: 760,
    minHeight: 640,
    show: false,
    title: "PNB Resume Prospective",
    backgroundColor: "#ffffff",
    icon: path.join(__dirname, "ui", "pnb-paws-logo-transparent.png"),
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      webSecurity: true,
      devTools: !app.isPackaged,
      spellcheck: true,
    },
  });
  window.webContents.setWindowOpenHandler(({ url }) => {
    if (externalUrl(url)) shell.openExternal(url).catch(() => {});
    return { action: "deny" };
  });
  window.webContents.on("will-navigate", (event, url) => {
    if (url.split("#")[0] !== `${origin}/`) event.preventDefault();
  });
  window.webContents.on("will-attach-webview", (event) =>
    event.preventDefault(),
  );
  window.webContents.on("did-fail-load", () =>
    console.error("The desktop interface could not load."),
  );
  window.webContents.on("render-process-gone", () =>
    console.error("The desktop interface stopped unexpectedly."),
  );
  window.webContents.once("did-finish-load", () =>
    console.log("PNB desktop interface loaded."),
  );
  window.once("ready-to-show", () => window.show());
  window.on("closed", () => {
    window = null;
  });
  window.loadURL(`${origin}/`);
}

app.on("activate", () => {
  if (BrowserWindow.getAllWindows().length === 0 && app.isReady()) openWindow();
});
app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
