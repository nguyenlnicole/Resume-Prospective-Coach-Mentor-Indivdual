export async function saveResumeDownload(blob, name) {
  if (window.PnbDownloads?.postMessage) {
    const base64 = await new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result.split(",")[1]);
      reader.onerror = () =>
        reject(new Error("The file could not be prepared. Please try again."));
      reader.readAsDataURL(blob);
    });
    window.PnbDownloads.postMessage(
      JSON.stringify({ name, type: blob.type, base64 }),
    );
    return;
  }
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
