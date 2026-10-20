import React, { useState } from 'react';
import { createResumeError } from './create-resume.js';
import './create-resume.css';
import MonthYearSelect from './MonthYearSelect.jsx';

export default function CreateResume({ value, onChange, onContinue, onBack, busy, error }) {
  const [validation,setValidation] = useState('');
  const update = (key,next) => onChange({...value,[key]:next});
  const row = (key,index,field,next) => update(key,value[key].map((item,i)=>i===index?{...item,[field]:next}:item));
  const input = (key,label,type='text') => <div className="field"><label htmlFor={`create-${key}`}>{label}</label><input id={`create-${key}`} type={type} maxLength={200} value={value[key]} onChange={e=>update(key,e.target.value)} /></div>;
  function next(event) { event.preventDefault(); const issue=createResumeError(value); setValidation(issue); if(issue)return; if(value.step<2)update('step',value.step+1);else onContinue(); }
  return <section className="create-resume card">
    <header className="create-resume-heading">
    <p className="eyebrow">CREATE A RESUME · STEP {value.step+1} OF 3</p>
    <h1>{['About you and your profession','Your experience','Education and skills'][value.step]}</h1>
    <p className="create-resume-intro">{['Tell us about yourself and choose the profession for your resume.','Tell us about the work you’ve done in your chosen profession.','Add your education, skills, and relevant training.'][value.step]}</p>
    <ol className="create-resume-steps" aria-label="Resume creation progress">{['Profession','Experience','Education'].map((label,index)=><li key={label} aria-current={index===value.step?'step':undefined} className={index<=value.step?'is-active':''}><span>{index+1}</span>{label}</li>)}</ol>
    </header>
    <form onSubmit={next}>
      {value.step===0 && <>
        <div className="create-resume-fields">{input('first','First name')}{input('last','Last name')}{input('email','Email address','email')}{input('phone','Phone · Optional','tel')}{input('location','City and state · Optional')}</div>
        <div className="field"><label htmlFor="create-profession">Which profession are you in or entering?</label><select id="create-profession" value={value.profession} onChange={e=>update('profession',e.target.value)}><option value="">Choose a profession</option>{['Technical','General','Medical Field','Educator'].map(item=><option key={item}>{item}</option>)}</select></div>
        {value.profession==='Technical' && <div className="field"><label htmlFor="create-level">Career level</label><select id="create-level" value={value.level} onChange={e=>update('level',e.target.value)}><option value="">Choose a level</option>{['Entry / Junior','Mid-level','Senior','Management'].map(item=><option key={item}>{item}</option>)}</select></div>}
      </>}
      {value.step===1 && <>
        <h2>Experience</h2><p>Add employment, internships, or volunteer work. Label volunteer roles clearly. No work history yet? You can continue without an entry.</p>
        {value.experience.map((job,index)=><section className="create-resume-detail" key={job.id}>
          <h3>Experience {index+1}</h3><div className="create-resume-fields">{['employer','role','start',...(job.current?[]:['end'])].map(key=><div className="field" key={key}><label htmlFor={`job-${index}-${key}`}>{{employer:'Employer or organization',role:'Role or job title',start:'Start date',end:'End date'}[key]}</label>{['start','end'].includes(key)?<MonthYearSelect id={`job-${index}-${key}`} label={key==='start'?'Start date':'End date'} value={job[key]||''} onChange={next=>row('experience',index,key,next)} required />:<input id={`job-${index}-${key}`} maxLength={160} value={job[key]||''} onChange={e=>row('experience',index,key,e.target.value)} />}</div>)}
          </div><label className="create-resume-check"><input type="checkbox" checked={Boolean(job.current)} onChange={e=>row('experience',index,'current',e.target.checked)} /> I still work here</label>
          <div className="field"><label htmlFor={`job-${index}-description`}>{{Technical:'What did you build, improve, or troubleshoot? Describe your contribution and results.',Educator:'How did you teach, support learners, or improve learning? Describe your contribution and results.','Medical Field':'How did you support patients, care teams, or healthcare services? Describe your responsibilities and results.',General:'What did you do? Tell us about responsibilities, contributions, and real results.'}[value.profession] || 'Describe your responsibilities, contributions, and real results.'}</label><textarea id={`job-${index}-description`} rows={4} maxLength={4000} value={job.description||''} onChange={e=>row('experience',index,'description',e.target.value)} /></div>
          <button type="button" className="text-button" onClick={()=>update('experience',value.experience.filter((_,i)=>i!==index))}>Remove experience</button>
        </section>)}
        {value.experience.length<10 && <button type="button" className="text-button" onClick={()=>update('experience',[...value.experience,{id:crypto.randomUUID()}])}>Add experience</button>}
      </>}
      {value.step===2 && <>
        <p>Add schools, degrees, or education programs. Include in-progress education; leave dates blank if you do not know them.</p>
        {value.education.map((item,index)=><section className="create-resume-detail" key={item.id}>
          <h3>Education {index+1}</h3><div className="create-resume-fields">{['school','qualification','date'].map(key=><div className="field" key={key}><label htmlFor={`education-${index}-${key}`}>{{school:'School or institution',qualification:'Degree, qualification, or program',date:'Completion or expected completion date · Optional'}[key]}</label>{key==='date'?<MonthYearSelect id={`education-${index}-${key}`} label="Completion date" value={item[key]||''} onChange={next=>row('education',index,key,next)} />:<input id={`education-${index}-${key}`} maxLength={200} value={item[key]||''} onChange={e=>row('education',index,key,e.target.value)} />}</div>)}
          </div><label className="create-resume-check"><input type="checkbox" checked={Boolean(item.current)} onChange={e=>row('education',index,'current',e.target.checked)} /> I am still studying here</label>
          <button type="button" className="text-button" onClick={()=>update('education',value.education.filter((_,i)=>i!==index))}>Remove education</button>
        </section>)}
        {value.education.length<10 && <button type="button" className="text-button" onClick={()=>update('education',[...value.education,{id:crypto.randomUUID()}])}>Add education</button>}
        {value.profession && <>
          <div className="field"><label htmlFor="create-skills">{{Technical:'Which technologies and tools have you used?',Educator:'Which teaching, classroom, or learner-support skills do you have?','Medical Field':'Which patient-care or healthcare administrative skills have you used?',General:'Which practical, customer-service, or workplace skills do you have?'}[value.profession]}</label><textarea id="create-skills" rows={3} maxLength={2000} value={value.skills} onChange={e=>update('skills',e.target.value)} /></div>
          <div className="field"><label htmlFor="create-credentials">Relevant licenses, certifications, or training · Optional</label><textarea id="create-credentials" rows={3} maxLength={2000} value={value.credentials} onChange={e=>update('credentials',e.target.value)} /></div>
          <p>Next, we’ll ask questions tailored to your profession, including relevant projects, credentials, and training.</p>
        </>}
        <div className="field">
          <label htmlFor="create-additional-languages">Do you speak any additional languages?</label>
          <select id="create-additional-languages" value={value.additionalLanguages || ''} onChange={e=>update('additionalLanguages',e.target.value)}>
            <option value="">Choose an answer · Optional</option><option value="yes">Yes</option><option value="no">No</option>
          </select>
        </div>
        {value.additionalLanguages==='yes' && <div className="field">
          <label htmlFor="create-languages">Which languages would you like to include on your resume?</label>
          <input id="create-languages" maxLength={500} value={value.languages || ''} placeholder="For example: Spanish, Vietnamese, ASL" onChange={e=>update('languages',e.target.value)} />
        </div>}
        <label className="consent"><input type="checkbox" checked={value.consent} onChange={e=>update('consent',e.target.checked)} /> Your resume data will be deleted unless you ask us to save it.</label>
        <p>This starts a new resume and uses one resume credit if your plan uses credits.</p>
      </>}
      {(validation||error)&&<p role="alert" className="invalid">{validation||error}</p>}
      <div className="page-actions"><button type="button" className="secondary" disabled={busy} onClick={()=>{setValidation('');value.step?update('step',value.step-1):onBack();}}>Back</button><button type="submit" disabled={busy}>{busy?'Preparing your resume…':value.step<2?'Continue':'Continue to profession questions'}</button></div>
    </form>
  </section>;
}
