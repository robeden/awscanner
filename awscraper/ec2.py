import boto3
import datetime
from dataclasses import dataclass
from enum import IntEnum
from typing import Optional, Collection, Dict

from awscraper import types


class InstanceState(IntEnum):
    # https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_InstanceState.html
    PENDING = 0
    RUNNING = 16
    SHUTTING_DOWN = 32
    TERMINATED = 48
    STOPPING = 64
    STOPPED = 80


def find_state_for_code(code: int) -> Optional[InstanceState]:
    for state in InstanceState:
        if state.value == code:
            return state
    return None


@dataclass
class InstanceInfo(types.IdentifiedResource):
    type: str
    state: InstanceState
    image_id: str
    private_ip: str
    public_ip: Optional[str]
    subnet_id: str
    vpc_id: str
    arch: str
    reservation_id: str
    volume_ids: Collection[str]
    launch_time: datetime.datetime


def gather(session: boto3.Session) -> Dict[str, InstanceInfo]:
    ec2_client = session.client("ec2")
    instance_map: Dict[str, InstanceInfo] = {}
    for page in ec2_client.get_paginator('describe_instances').paginate():
        reservations = page['Reservations']
        for reservation in reservations:
            instances = reservation['Instances']
            for i in instances:
                instance_map[i['InstanceId']] = InstanceInfo(
                    i['InstanceId'],
                    types.parse_tags(i.get('Tags')),
                    i['InstanceType'],
                    find_state_for_code(i['State']['Code']),
                    i['ImageId'],
                    i['PrivateIpAddress'],
                    i.get('PublicIpAddress'),
                    i['SubnetId'],
                    i['VpcId'],
                    i['Architecture'],
                    reservation['ReservationId'],
                    [bdm['VolumeId'] for bdm in i.get("Ebs", {}).get("BlockDeviceMappings", [])],
                    i['LaunchTime']
                )
    return instance_map
