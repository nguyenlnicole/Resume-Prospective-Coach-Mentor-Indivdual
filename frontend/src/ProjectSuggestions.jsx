import React, {useEffect,useState} from 'react';
import {githubUsername} from './rules';
import {suggestProjects} from './project-suggestions';
import {fetchProjectDetails} from './new-projects.js';

export default function ProjectSuggestions({profile,projects,target,disabled,selected=[],onAdd}) {
  const [suggestions,setSuggestions]=useState([]);
  const [status,setStatus]=useState('');
  const [loading,setLoading]=useState(false);
  const [adding,setAdding]=useState('');
  const [retry,setRetry]=useState(0);
  const username=githubUsername(profile), projectKey=JSON.stringify(projects);
  useEffect(()=>{
    setSuggestions([]);setStatus('');setLoading(false);
    if(disabled || !username) return;
    const controller=new AbortController();
    let active=true;
    const timeout=setTimeout(()=>controller.abort(),10000);
    setLoading(true);
    const timer=setTimeout(async()=>{
      try {
        const response=await fetch(`https://api.github.com/users/${encodeURIComponent(username)}/repos?per_page=100&sort=updated`,{signal:controller.signal,headers:{Accept:'application/vnd.github+json'}});
        if(!response.ok)throw new Error(response.status===403 || response.status===429 ? 'GitHub is limiting requests. Try again shortly or add your project details manually.' : 'Could not read this GitHub profile. Check that the username and repositories are public.');
        const repos=await response.json();
        if(!Array.isArray(repos))throw new Error('GitHub did not return a repository list. Please try again.');
        if(active){
          const found=suggestProjects(JSON.parse(projectKey),repos,target);
          setSuggestions(found);
          if(!found.length)setStatus('No additional public projects were found. You can still add a project manually.');
        }
      } catch(error) {
        if(active)setStatus(error.name==='AbortError' ? 'GitHub took too long to respond. Try again or add your project manually.' : error.message);
      } finally {clearTimeout(timeout);if(active)setLoading(false);}
    },800);
    return()=>{active=false;controller.abort();clearTimeout(timer);clearTimeout(timeout);};
  },[username,projectKey,target,disabled,retry]);
  async function add(repo){
    setAdding(repo.url);setStatus('');
    try {
      const details=await fetchProjectDetails(repo.url);
      onAdd({...details,id:crypto.randomUUID(),onGithub:'yes',confirmed:false});
      setStatus('Project details filled in. On the next page, review them, describe your contribution, and confirm before adding them to your resume.');
    } catch(error){setStatus(error.message);} finally{setAdding('');}
  }
  if(disabled || !username)return null;
  return <section className="card profile-help" aria-label="GitHub project suggestions">
    <h2>{projects.length<3 ? 'Add projects from GitHub' : 'Other projects worth considering'}</h2>
    <p>Choose a public repository to fill its available details. Review the details and confirm your own work before it is added to your resume.</p>
    {loading && <p role="status">Looking for public GitHub projects…</p>}
    {status && <p role="status">{status}</p>}
    {suggestions.map(repo=>{
      const chosen=selected.some(item=>item.repo?.replace(/\/$/,'')===repo.url.replace(/\/$/,''));
      return <article key={repo.url}>
        <h3><a href={repo.url} target="_blank" rel="noreferrer">{repo.name}</a></h3>
        <p>{repo.description || 'No repository description is available. Add one when you review the project.'}</p>
        {repo.replaceIndex!=null && <p>Consider instead of <strong>{projects[repo.replaceIndex].split('\n')[0].split('|')[0]}</strong>.</p>}
        <button type="button" className="button secondary" disabled={Boolean(adding)||chosen||selected.length>=5} onClick={()=>add(repo)}>{chosen?'Selected for review':adding===repo.url?'Reading project…':'Use this project'}</button>
      </article>;
    })}
    {!loading && <button type="button" className="text-button" onClick={()=>setRetry(value=>value+1)}>Check GitHub again</button>}
  </section>;
}