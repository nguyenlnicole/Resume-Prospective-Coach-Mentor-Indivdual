export function storedEmail(key) {
  try {
    return localStorage.getItem(key) || "";
  } catch {
    return "";
  }
}

export function saveRememberedEmail(key, email, remember) {
  try {
    if (remember && email.trim()) localStorage.setItem(key, email.trim());
    else localStorage.removeItem(key);
  } catch {}
}
