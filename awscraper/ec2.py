import boto3
import datetime
from dataclasses import dataclass
from enum import IntEnum
from typing import Optional, Collection, Dict, Iterator

from awscraper.util import IdentifiedResource, parse_tags


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
class InstanceInfo(IdentifiedResource):
    type: str
    state: InstanceState
    image_id: str
    private_ip: str
    public_ip: Optional[str]
    subnet_id: str
    vpc_id: str
    arch: str
    reservation_id: str
    launch_time: datetime.datetime
    volume_ids: Collection[str]
    security_group_ids: Collection[str]

    def is_running(self):
        return self.state in [InstanceState.RUNNING, InstanceState.PENDING]


@dataclass
class EBSInfo(IdentifiedResource):
    attached_instances: Collection[str]
    state: str
    encrypted: bool
    size: int
    iops: int
    throughput: Optional[int]
    type: str


def gather_instances(session: boto3.Session, client=None) -> Iterator[InstanceInfo]:
    ec2_client = client if client is not None else session.client("ec2")
    for page in ec2_client.get_paginator('describe_instances').paginate():
        reservations = page['Reservations']
        for reservation in reservations:
            instances = reservation['Instances']
            for i in instances:
                yield InstanceInfo(
                    i['InstanceId'],
                    parse_tags(i.get('Tags')),
                    i['InstanceType'],
                    find_state_for_code(i['State']['Code']),
                    i['ImageId'],
                    i['PrivateIpAddress'],
                    i.get('PublicIpAddress'),
                    i['SubnetId'],
                    i['VpcId'],
                    i['Architecture'],
                    reservation['ReservationId'],
                    i['LaunchTime'],
                    [bdm['VolumeId'] for bdm in i.get("Ebs", {}).get("BlockDeviceMappings", [])],
                    [sg['GroupId'] for sg in i["SecurityGroups"]]
                )


def gather_ebs(session: boto3.Session, client=None) -> Iterator[EBSInfo]:
    ec2_client = client if client is not None else session.client("ec2")
    for page in ec2_client.get_paginator('describe_volumes').paginate():
        # {
        #     'Volumes': [
        #         {
        #             'Attachments': [
        #                 {
        #                     'AttachTime': datetime(2015, 1, 1),
        #                     'Device': 'string',
        #                     'InstanceId': 'string',
        #                     'State': 'attaching'|'attached'|'detaching'|'detached'|'busy',
        #                     'VolumeId': 'string',
        #                     'DeleteOnTermination': True|False
        #                 },
        #             ],
        #             'AvailabilityZone': 'string',
        #             'CreateTime': datetime(2015, 1, 1),
        #             'Encrypted': True|False,
        #             'KmsKeyId': 'string',
        #             'OutpostArn': 'string',
        #             'Size': 123,
        #             'SnapshotId': 'string',
        #             'State': 'creating'|'available'|'in-use'|'deleting'|'deleted'|'error',
        #             'VolumeId': 'string',
        #             'Iops': 123,
        #             'Tags': [
        #                 {
        #                     'Key': 'string',
        #                     'Value': 'string'
        #                 },
        #             ],
        #             'VolumeType': 'standard'|'io1'|'io2'|'gp2'|'sc1'|'st1'|'gp3',
        #             'FastRestored': True|False,
        #             'MultiAttachEnabled': True|False,
        #             'Throughput': 123
        #         },
        #     ],
        #
        # }
        volumes = page['Volumes']
        for v in volumes:
            yield EBSInfo(
                v['VolumeId'],
                parse_tags(v.get('Tags')),
                [a['InstanceId'] for a in v['Attachments']],
                v['State'],
                v['Encrypted'],
                v['Size'],
                v['Iops'],
                v.get('Throughput'),
                v['VolumeType']
            )

