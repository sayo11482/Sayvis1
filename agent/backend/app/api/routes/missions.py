from fastapi import APIRouter, HTTPException
from ...models.schemas import MissionCreate, MissionResponse, MissionStatus, MissionTask
from typing import List
from datetime import datetime
import uuid
import logging

router = APIRouter()
logger = logging.getLogger(__name__)

# In-memory storage for demo (in production use Postgres)
_missions_db: dict = {}

@router.post("/missions", response_model=MissionResponse, tags=["Missions"])
async def create_mission(mission: MissionCreate):
    """
    Create a new mission - AI will auto-breakdown into tasks
    """
    from ...main import get_services
    
    mission_id = str(uuid.uuid4())
    
    # Auto-breakdown using AI if available
    tasks = []
    try:
        services = get_services()
        orchestrator = services.get("orchestrator")
        
        if orchestrator:
            breakdown_prompt = f"""
Break down this mission into 3-5 actionable tasks:

Title: {mission.title}
Description: {mission.description}
Priority: {mission.priority}

Return as JSON list: [{{"title": "task title", "priority": "medium"}}]
"""
            result = await orchestrator.process_message(
                message=breakdown_prompt,
                session_id=f"mission-{mission_id}",
                language="en",
                use_tools=False,
                use_memory=False,
                use_rag=False
            )
            
            # Try to parse tasks from response
            import json, re
            json_match = re.search(r'\[.*\]', result["response"], re.DOTALL)
            if json_match:
                try:
                    parsed = json.loads(json_match.group(0))
                    for i, t in enumerate(parsed[:5]):
                        tasks.append(MissionTask(
                            id=str(uuid.uuid4()),
                            title=t.get("title", f"Task {i+1}"),
                            completed=False,
                            priority=t.get("priority", "medium")
                        ))
                except:
                    pass
    except Exception as e:
        logger.warning(f"Mission auto-breakdown failed: {e}")

    # Fallback tasks if AI failed
    if not tasks:
        tasks = [
            MissionTask(id=str(uuid.uuid4()), title=f"Research: {mission.title}", completed=False, priority="high"),
            MissionTask(id=str(uuid.uuid4()), title="Create action plan", completed=False, priority="high"),
            MissionTask(id=str(uuid.uuid4()), title="Execute and track progress", completed=False, priority="medium"),
        ]

    mission_resp = MissionResponse(
        id=mission_id,
        title=mission.title,
        description=mission.description,
        status=MissionStatus.ACTIVE,
        priority=mission.priority,
        tasks=tasks,
        progress=0.0,
        created_at=datetime.utcnow(),
        updated_at=datetime.utcnow()
    )
    
    _missions_db[mission_id] = mission_resp
    
    # Trigger n8n workflow
    try:
        from ...main import get_services
        services = get_services()
        n8n = services.get("n8n")
        if n8n:
            await n8n.trigger_sayvis_workflows("new_mission", {
                "mission_id": mission_id,
                "title": mission.title,
                "description": mission.description,
                "tasks_count": len(tasks)
            })
    except Exception as e:
        logger.warning(f"n8n mission trigger failed: {e}")

    return mission_resp

@router.get("/missions", response_model=List[MissionResponse], tags=["Missions"])
async def list_missions(status: str = None):
    missions = list(_missions_db.values())
    if status:
        missions = [m for m in missions if m.status.value == status.upper()]
    return missions

@router.get("/missions/{mission_id}", response_model=MissionResponse, tags=["Missions"])
async def get_mission(mission_id: str):
    if mission_id not in _missions_db:
        raise HTTPException(status_code=404, detail="Mission not found")
    return _missions_db[mission_id]

@router.patch("/missions/{mission_id}/tasks/{task_id}", response_model=MissionResponse, tags=["Missions"])
async def toggle_task(mission_id: str, task_id: str, completed: bool = True):
    if mission_id not in _missions_db:
        raise HTTPException(status_code=404, detail="Mission not found")
    
    mission = _missions_db[mission_id]
    
    for task in mission.tasks:
        if task.id == task_id:
            task.completed = completed
            break
    else:
        raise HTTPException(status_code=404, detail="Task not found")
    
    # Recalculate progress
    if mission.tasks:
        completed_count = sum(1 for t in mission.tasks if t.completed)
        mission.progress = completed_count / len(mission.tasks)
        
        if mission.progress >= 1.0:
            mission.status = MissionStatus.COMPLETED
    
    mission.updated_at = datetime.utcnow()
    _missions_db[mission_id] = mission
    
    return mission

@router.delete("/missions/{mission_id}", tags=["Missions"])
async def delete_mission(mission_id: str):
    if mission_id not in _missions_db:
        raise HTTPException(status_code=404, detail="Mission not found")
    
    del _missions_db[mission_id]
    return {"deleted": True, "mission_id": mission_id}
