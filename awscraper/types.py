from dataclasses import dataclass
from typing import Dict


@dataclass
class IdentifiedResource:
    id: str
    tags: Dict[str, str]


# 'Tags': [
#     {
#         'Key': 'string',
#         'Value': 'string'
#     },
# ],
def parse_tags(tags) -> Dict[str, str]:
    if tags is None:
        return {}
    return {t['Key']: t['Value'] for t in tags}
