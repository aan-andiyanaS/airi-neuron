import sys, json
from pathlib import Path
from graphify.build import build_from_json
from graphify.report import generate

# Load graph
extraction = json.loads(Path('graphify-out/.graphify_extract.json').read_text(encoding="utf-8"))
G = build_from_json(extraction, root='.', directed=False)

# Load analysis
analysis = json.loads(Path('graphify-out/.graphify_analysis.json').read_text(encoding="utf-8"))
communities = {int(k): v for k, v in analysis['communities'].items()}
cohesion = {int(k): v for k, v in analysis['cohesion'].items()}
gods = analysis['gods']
surprises = analysis['surprises']
questions = analysis['questions']

detection = json.loads(Path('graphify-out/.graphify_detect.json').read_text(encoding="utf-8"))
tokens = {'input': extraction.get('input_tokens', 0), 'output': extraction.get('output_tokens', 0)}

# Load labels
labels = json.loads(Path('graphify-out/.graphify_labels.json').read_text(encoding="utf-8"))
labels = {int(k): v for k, v in labels.items()}

report = generate(G, communities, cohesion, labels, gods, surprises, detection, tokens, '.', suggested_questions=questions)
Path('graphify-out/GRAPH_REPORT.md').write_text(report, encoding="utf-8")
print('Report regenerated with labels.')
