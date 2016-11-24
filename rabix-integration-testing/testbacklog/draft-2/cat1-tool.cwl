class: CommandLineTool
description: "Grep desired string from desired file"
inputs:
  - id: "#stringPattern"
    type: string
    inputBinding:
      position: 1
  - id: "#fileToGrep"
    type: File
    inputBinding:
      position: 2 
outputs:
  - id: "#output"
    type: File
    outputBinding:
      glob: out.txt
baseCommand: rev
successCodes: [0,1]
stdout: out.txt
