/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
buildPlugin(
  forkCount: '1C', // Run 1 test process per core
  useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
  configurations: [
    [platform: 'linux', jdk: 21],
    [platform: 'windows', jdk: 17],
])
