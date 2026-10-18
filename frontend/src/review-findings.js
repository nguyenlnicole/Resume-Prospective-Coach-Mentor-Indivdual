// Keep successful reviews informative when no improvements were identified.
export function visibleReviewFindings(findings, includePassing = false) {
  const items = Array.isArray(findings) ? findings.filter(item => item && typeof item === 'object') : [];
  const improvements = items.filter(item => item.status !== 'pass');
  return includePassing || improvements.length === 0 ? items : improvements;
}
