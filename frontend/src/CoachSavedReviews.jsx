import React, {useState, useRef, useEffect} from "react";
import {Folder, X} from "lucide-react";
import {request, post} from "./api";
export default function CoachSavedReviews({onOpen,onResumeProgress}) {
 const [open,setOpen]=useState(false),[items,setItems]=useState([]),[error,setError]=useState(""),[busy,setBusy]=useState(false);
 const dialog=useRef(null);
 useEffect(()=>{if(open)dialog.current?.showModal();else dialog.current?.close();},[open]);
 async function show(){setOpen(true);setBusy(true);setError("");try{const [reviews,draft]=await Promise.all([request("/coach/saved"),request("/workflow-draft")]);setItems([...(draft.items || []).map(item => ({...item,workflow:true})),...reviews]);}catch(e){setError(e.message);}finally{setBusy(false);}}
 async function remove(item){const {id,workflow}=item;setBusy(true);try{await request(workflow ? `/workflow-draft/saved/${id}` : `/coach/saved/${id}`,{method:"DELETE"});setItems(items.filter(x=>x.id!==id || x.workflow!==workflow));}catch(e){setError(e.message);}finally{setBusy(false);}}
 async function resume(item){const {id,workflow}=item;setBusy(true);try{if(workflow){if(await onResumeProgress(id))setOpen(false);else setError("This saved resume could not be opened. Please try again.");return;}const data=await post(`/coach/saved/${id}/open`,{});onOpen(data);setOpen(false);}catch(e){setError(e.message);}finally{setBusy(false);}}
 return <><button className="coach-recent-toggle" onClick={show}><Folder size={20}/> Saved</button>
 <dialog ref={dialog} className="saved-review-dialog" onCancel={()=>setOpen(false)}><button className="saved-close" aria-label="Close saved reviews" onClick={()=>setOpen(false)}><X/></button><h2>Saved for later</h2><p>Choose a review to return to “Ready to build?”</p>{busy&&<p role="status">Loading…</p>}{error&&<p role="alert">{error}</p>}{!busy&&!items.length&&<p>No saved reviews yet.</p>}
 {items.map(item=><div className="saved-review-row" key={`${item.workflow ? "draft" : "review"}-${item.id}`}><button disabled={busy} onClick={()=>resume(item)}><Folder size={22}/><span>{item.firstName}<small>{new Date(item.savedAt).toLocaleDateString()}</small></span></button><button disabled={busy} aria-label={`Delete saved review for ${item.firstName}`} onClick={()=>remove(item)}><X size={18}/> Delete</button></div>)}
 </dialog></>;
}
